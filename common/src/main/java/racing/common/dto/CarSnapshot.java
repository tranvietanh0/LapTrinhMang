package racing.common.dto;

import java.io.Serializable;

/**
 * Trạng thái một xe do server xác nhận, nằm trong RaceState.
 *
 * @param username  chủ xe
 * @param distance  quãng đường đã đi (mét), đã cắt ngưỡng
 * @param lane      làn hiện tại
 * @param speed     tốc độ (km/h), bằng 0 khi đang bị choáng sau va chạm
 * @param stunned   true trong COLLISION_STUN_MS sau khi va chạm (client nháy đỏ)
 * @param finished  true khi đã qua vạch đích
 */
public record CarSnapshot(String username, double distance, int lane, double speed,
                          boolean stunned, boolean finished) implements Serializable {
    private static final long serialVersionUID = 1L;
}
