package racing.client.model;

import racing.common.dto.PlayerInfo;
import racing.common.dto.PlayerStatus;

import java.util.List;
import java.util.Optional;

/**
 * Trạng thái phía client: tôi là ai, ai đang online, tôi có đang chờ ai trả lời lời mời không.
 * Chỉ dùng trên luồng Swing.
 */
public final class ClientState {

    private PlayerInfo me;
    private List<PlayerInfo> online = List.of();
    /** Tên người tôi đã mời và đang chờ trả lời; null khi không có. */
    private String pendingInviteTarget;

    public PlayerInfo me() {
        return me;
    }

    public void setMe(PlayerInfo me) {
        this.me = me;
    }

    public List<PlayerInfo> online() {
        return online;
    }

    /** Cập nhật danh sách online; đồng thời làm mới thông tin của chính mình (điểm, trạng thái). */
    public void setOnline(List<PlayerInfo> list) {
        this.online = list == null ? List.of() : List.copyOf(list);
        if (me != null) {
            find(me.username()).ifPresent(p -> me = p);
        }
    }

    public boolean isSelf(PlayerInfo p) {
        return me != null && p != null && me.username().equals(p.username());
    }

    public Optional<PlayerInfo> find(String username) {
        return online.stream().filter(p -> p.username().equals(username)).findFirst();
    }

    /** Có thể mời người này không: không phải mình, đang rảnh, và tôi chưa mời ai khác. */
    public boolean canInvite(PlayerInfo p) {
        return p != null && !isSelf(p) && p.status() == PlayerStatus.FREE && !hasPendingInvite();
    }

    public boolean hasPendingInvite() {
        return pendingInviteTarget != null;
    }

    public void markInviteSent(String target) {
        this.pendingInviteTarget = target;
    }

    public String pendingInviteTarget() {
        return pendingInviteTarget;
    }

    public void clearPendingInvite() {
        this.pendingInviteTarget = null;
    }
}
