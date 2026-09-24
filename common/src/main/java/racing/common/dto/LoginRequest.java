package racing.common.dto;

import java.io.Serializable;

/** C→S LOGIN. Mật khẩu gửi dạng thô qua TCP nội bộ; server băm và so với password_hash. */
public record LoginRequest(String username, String password) implements Serializable {
    private static final long serialVersionUID = 1L;
}
