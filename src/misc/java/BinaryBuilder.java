package misc.java;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class BinaryBuilder {
    public static void main(String[] args) throws IOException {
        BinaryBuilder builder = new BinaryBuilder(Path.of(args[0]));
    }

    @Getter
    @AllArgsConstructor
    @ToString()
    class Pair {
        String type;
        String name;
    }

    BinaryBuilder(Path p) throws IOException {
        List<String> lines = Files.readAllLines(p).stream().map(String::trim).filter(x -> !x.isEmpty()).toList();
        List<String> header = List.of(lines.getFirst().split(" "));
        if (!header.getFirst().equals("class")) {
            System.out.println(header.getFirst());
            throw new RuntimeException("EXPECTED CLASS");
        }
        String className = header.get(1);
        List<Pair> pairs = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            List<String> s = List.of(lines.get(i).split(" "));
            pairs.add(new Pair(s.get(0), s.get(1)));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("""
                import lombok.AllArgsConstructor;
                import lombok.Getter;
                import lombok.Setter;
                
                import java.io.IOException;
                import java.nio.ByteBuffer;
                import java.nio.channels.ByteChannel;
                import java.nio.charset.StandardCharsets;
                import RAFT.RPC.Type.RPCMessage;
                
                """);
        sb.append("""
                @Getter
                @Setter
                @ToString
                @AllArgsConstructor
                @NoArgsConstructor
                """);
        sb.append(String.format("public class %s implements RPCMessage {\n", className));
        for (var x : pairs) {
            if (x.type.equals("String")) {
                x.type = "RPCString";
            }
            sb.append(String.format("\t%s %s;\n", x.type, x.name));
        }
        sb.append(String.format("\t%s(ByteChannel channel) throws IOException {\n", className));
        sb.append("\t\tget(channel);\n");
        sb.append("\t}\n");
        List<String> put = new ArrayList<>();
        int sum = 0;
        List<Pair> temp = new ArrayList<>();
        for (int i = 0; i < pairs.size(); i++) {
            Pair pair = pairs.get(i);
            if (!isPrimitive(pair.type)) {
                if (sum != 0) {
                    put.add(String.format("ByteBuffer buffer=new ByteBuffer(%d);",sum));
                    put.add("buffer.clear();");
                    put.addAll(temp.stream().map(x -> fromPair(x, "put")).toList());
                    put.add("buffer.flip();");
                    put.add("channel.write(buffer);\n");
                }

                sum = 0;
                temp = new ArrayList<>();
                put.add(String.format("%s.put(channel);", pair.name));
            } else {
                temp.add(pair);
                sum+=primitiveSize(pair.type);
            }
        }
        if (sum != 0) {
            put.add(String.format("ByteBuffer buffer=ByteBuffer.allocate(%d);",sum));
            put.add("buffer.clear();");
            put.addAll(temp.stream().map(x -> fromPair(x, "put")).toList());
            put.add("buffer.flip();");
            put.add("channel.write(buffer);\n");
        }

        sb.append(addFunction("put", put));

        put = new ArrayList<>();
        sum = 0;
        temp = new ArrayList<>();
        for (int i = 0; i < pairs.size(); i++) {
            Pair pair = pairs.get(i);
            if (!isPrimitive(pair.type)) {
                if (sum != 0) {
                    put.add(String.format("ByteBuffer buffer=ByteBuffer.allocate(%d);",sum));
                    put.add("buffer.clear();");
                    put.add("channel.read(buffer);\n");
                    put.add("buffer.flip();");
                    put.addAll(temp.stream().map(x -> fromPair(x, "get")).toList());
                }

                sum = 0;
                temp = new ArrayList<>();
                put.add(String.format("%s = new %s(channel);", pair.name,pair.type));
            } else {
                temp.add(pair);
                sum+=primitiveSize(pair.type);
            }
        }
        if (sum != 0) {
            put.add(String.format("ByteBuffer buffer=ByteBuffer.allocate(%d);",sum));
            put.add("buffer.clear();");
            put.addAll(temp.stream().map(x -> fromPair(x, "get")).toList());
            put.add("buffer.flip();");
            put.add("channel.write(buffer);\n");
        }

        sb.append(addFunction("get", put));
        sb.append("}\n");
        System.out.println(sb);
    }

    boolean isPrimitive(String x) {
        return List.of("long", "int", "short", "byte", "boolean").contains(x);
    }

    public String fromPair(Pair x, String verb) {
        String f = "";
        if (x.type.equals("long")) {
            f = "Long";
        }
        if (x.type.equals("int")) {
            f = "Int";
        }

        if (x.type.equals("byte")) {
            f = "";
        }

        if (x.type.equals("short")) {
            f = "Short";
        }
        if (x.type.equals("boolean")) {
            f = "";
        }
        if(verb.equals("put"))
            return String.format("buffer.%s%s(%s);", verb, f, x.name);
        else{
            return String.format("%s=buffer.%s%s();", x.name,verb, f);
        }
    }

    int primitiveSize(String x) {
        if (x.equals("long")) {
            return 8;
        }
        if (x.equals("int")) {
            return 4;
        }

        if (x.equals("byte")) {
            return 1;
        }

        if (x.equals("short")) {
            return 2;
        }
        if (x.equals("boolean")) {
            return 1;
        }
        return 0;
    }

    String addFunction(String name, List<String> body) {
        return String.format("\t@Override\n\tpublic void %s(ByteChannel channel) throws IOException {\n%s\n\t}\n", name, body.stream().map(x -> "\t\t" + x + "\n").reduce("", String::concat));
    }
}
