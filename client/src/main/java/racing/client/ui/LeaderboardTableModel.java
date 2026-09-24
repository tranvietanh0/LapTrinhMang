package racing.client.ui;

import racing.common.dto.RankRow;

import javax.swing.table.AbstractTableModel;
import java.util.List;

/** Model bảng xếp hạng: Hạng, Tên, Điểm, Thắng, Thua, Hòa. Giữ nguyên thứ tự server gửi. */
public final class LeaderboardTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Hạng", "Tên", "Điểm", "Thắng", "Thua", "Hòa"};

    private List<RankRow> rows = List.of();

    public void setRows(List<RankRow> rows) {
        this.rows = rows == null ? List.of() : List.copyOf(rows);
        fireTableDataChanged();
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
        return columnIndex == 1 ? String.class : Integer.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        RankRow r = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> r.rank();
            case 1 -> r.username();
            case 2 -> r.points();
            case 3 -> r.wins();
            case 4 -> r.losses();
            default -> r.draws();
        };
    }
}
