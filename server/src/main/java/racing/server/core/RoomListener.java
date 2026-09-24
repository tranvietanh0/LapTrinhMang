package racing.server.core;

/** RoomManager nghe để hẹn giờ đếm ngược khi ván mới bắt đầu và dọn phòng khi đóng. */
public interface RoomListener {

    void roundStarted(Room room);

    void roomClosed(Room room);
}
