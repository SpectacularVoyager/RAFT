package Encoding.Binary.Test;

import Encoding.Binary.Serialize;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RPCResponse {
    @Serialize
    public int a;
    @Serialize
    public long b;
    @Serialize
    public String s;
    @Serialize
    public RPCID id;

}
