package racing.common.dto;

import java.io.Serializable;

/**
 * S→C LOGIN_RESULT.
 *
 * @param ok      true nếu đăng nhập thành công
 * @param message lý do khi thất bại ("Sai tài khoản hoặc mật khẩu", "Tài khoản đang đăng nhập ở nơi khác")
 * @param me      thông tin người chơi vừa đăng nhập (null khi thất bại)
 */
public record LoginResult(boolean ok, String message, PlayerInfo me) implements Serializable {
    private static final long serialVersionUID = 1L;

    public static LoginResult success(PlayerInfo me) {
        return new LoginResult(true, "OK", me);
    }

    public static LoginResult fail(String message) {
        return new LoginResult(false, message, null);
    }
}
