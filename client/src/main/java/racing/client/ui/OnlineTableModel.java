package racing.client.ui;

import racing.common.dto.PlayerInfo;
import racing.common.dto.PlayerStatus;

import javax.swing.table.AbstractTableModel;
import java.util.List;

/** Model bảng danh sách online trong sảnh: Tên, Điểm, Thắng, Trạng thái. Không phụ thuộc Swing hiển thị. */
public final class OnlineTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Tên", "Điểm", "Thắng", "Trạng thái"};

    private List<PlayerInfo> rows = List.of();
    private String selfUsername;

    /** Thay toàn bộ dữ liệu; mình luôn được xếp lên đầu và gắn nhãn "(bạn)". */
    public void setRows(List<PlayerInfo> players, String selfUsername) {
        this.selfUsername = selfUsername;
        List<PlayerInfo> sorted = players == null ? List.of() : players.stream()
                .sorted((a, b) -> {
                    boolean sa = a.username().equals(selfUsername);
                    boolean sb = b.username().equals(selfUsername);
                    if (sa != sb) {
                        return sa ? -1 : 1;
                    }
                    return a.username().compareToIgnoreCase(b.username());
                })
                .toList();
        this.rows = sorted;
        fireTableDataChanged();
    }

    public PlayerInfo playerAt(int row) {
        return row >= 0 && row < rows.size() ? rows.get(row) : null;
    }

    public boolean isSelfRow(int row) {
        PlayerInfo p = playerAt(row);
        return p != null && p.username().equals(selfUsername);
    }

    /** Chỉ số dòng của một người, hoặc -1. */
    public int rowOf(String username) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).username().equals(username)) {
                return i;
            }
        }
        return -1;
    }

    public static String statusText(PlayerStatus s) {
        return s == PlayerStatus.IN_MATCH ? "Đang thi đấu" : "Rảnh";
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 1 || columnIndex == 2 ? Integer.class : String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        PlayerInfo p = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> isSelfRow(rowIndex) ? p.username() + " (bạn)" : p.username();
            case 1 -> p.points();
            case 2 -> p.wins();
            default -> statusText(p.status());
        };
    }
}
