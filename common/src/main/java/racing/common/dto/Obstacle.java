package racing.common.dto;

import java.io.Serializable;

/**
 * Một chướng ngại vật trên đường đua. Hai đường đua nhận cùng danh sách nên giống nhau.
 *
 * @param lane     làn 0..GameConfig.LANES-1
 * @param position vị trí đầu chướng ngại vật tính từ vạch xuất phát (mét)
 */
public record Obstacle(int lane, double position) implements Serializable {
    private static final long serialVersionUID = 1L;
}
