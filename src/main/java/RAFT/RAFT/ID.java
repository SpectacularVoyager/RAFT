package RAFT.RAFT;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ID {
    private long id;
    private String host;
    private int port;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ID) {
            return ((ID) obj).id == this.id;
        }
        return false;
    }
}
