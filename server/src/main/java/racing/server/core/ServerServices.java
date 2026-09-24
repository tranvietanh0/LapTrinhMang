package racing.server.core;

/** Bó các dịch vụ lõi mà mỗi ClientHandler cần, để GameServer tạo một lần và chia sẻ. */
public record ServerServices(SessionManager sessions, InviteManager invites, RoomManager rooms,
                             AccountService accounts) {
}
