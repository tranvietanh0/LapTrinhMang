package racing.client.ui;

import racing.common.dto.EndReason;
import racing.common.dto.MatchOutcome;
import racing.common.dto.MatchRow;

import javax.swing.table.AbstractTableModel;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Model bảng lịch sử trận: Thời gian, Đối thủ, Kết quả, Lý do. */
public final class HistoryTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Thời gian", "Đối thủ", "Kết quả", "Lý do"};
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private List<MatchRow> rows = List.of();

    public void setRows(List<MatchRow> rows) {
        this.rows = rows == null ? List.of() : List.copyOf(rows);
        fireTableDataChanged();
    }

    public static String outcomeText(MatchOutcome o) {
        if (o == null) {
            return "";
        }
        return switch (o) {
            case WIN -> "Thắng";
            case LOSE -> "Thua";
            case DRAW -> "Hòa";
            case ABORTED -> "Hủy";
        };
    }

    public static String reasonText(EndReason r) {
        if (r == null) {
            return "";
        }
        return switch (r) {
            case FINISH -> "Về đích";
            case DRAW -> "Cùng về đích";
            case QUIT -> "Thoát trận";
            case DISCONNECT -> "Mất kết nối";
            case ABORTED -> "Hủy trận";
        };
    }

    public static String timeText(long epochMillis) {
        if (epochMillis <= 0) {
            return "";
        }
        return TIME.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
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
    public Object getValueAt(int rowIndex, int columnIndex) {
        MatchRow r = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> timeText(r.startedAtMillis());
            case 1 -> r.opponentUsername();
            case 2 -> outcomeText(r.outcome());
            default -> reasonText(r.reason());
        };
    }
}
