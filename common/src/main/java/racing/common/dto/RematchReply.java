package racing.common.dto;

import java.io.Serializable;

/** C→S REMATCH_REPLY. agree = true là muốn thi đấu tiếp. Chỉ khi cả hai true server mới tạo ván mới. */
public record RematchReply(int roomId, boolean agree) implements Serializable {
    private static final long serialVersionUID = 1L;
}
