package racing.common.dto;

import java.io.Serializable;

/**
 * C→S CAR_STATE, client gửi mỗi tick (20 lần / giây) trạng thái xe của mình.
 * Server coi đây là "đề nghị": cắt về ngưỡng nếu vượt MAX_SPEED hoặc đi quá xa trong một tick.
 *
 * @param distance quãng đường đã đi (mét)
 * @param lane     làn hiện tại 0..LANES-1
 * @param speed    tốc độ (km/h)
 */
public record CarState(double distance, int lane, double speed) implements Serializable {
    private static final long serialVersionUID = 1L;
}
