package org.starcoin.thor.manager

import io.ktor.features.NotFoundException
import org.starcoin.thor.core.GameBaseInfo
import org.starcoin.thor.core.Room

//all userId
class RoomManager {
    private val rooms = mutableMapOf<String, Room>()
    private val roomLock = java.lang.Object()

    private val roomSet = mutableSetOf<String>()
    private val game2Room = mutableMapOf<String, ArrayList<String>>()
    private val joinLock = java.lang.Object()

    fun createRoom(game: GameBaseInfo, deposit: Long, time: Long, userId: String? = null): Room {
        val room = when (deposit > 0) {
            false -> {
                Room(game.hash, 0, time)
            }
            true -> {
                Room(game.hash, deposit, time)
            }
        }
        userId?.let { room.addPlayer(userId) }
        synchronized(roomLock) {
            roomSet.add(room.roomId)
            when (game2Room[game.hash]) {
                null -> {
                    val list = ArrayList<String>()
                    list.add(room.roomId)
                    game2Room[game.hash] = list
                }
                else -> {
                    game2Room[game.hash]!!.add(room.roomId)
                }
            }
            rooms[room.roomId] = room
        }
        return room
    }

    fun queryRoomListByGame(gameId: String): List<Room>? {
        val roomIds = game2Room[gameId]
        roomIds?.let { return rooms.filterKeys { roomIds.contains(it) }.values.toList() }
        return null
    }

    fun queryRoomList(begin: Int, end: Int): List<Room> {
        val keys = roomSet.toList().subList(begin, end).toSet()
        return rooms.filterKeys { keys.contains(it) }.values.toList()
    }

    fun count(): Int {
        return this.rooms.size
    }

    fun queryRoomOrNull(roomId: String): Room? {
        return rooms[roomId]
    }

    fun queryRoomNotNull(roomId: String): Room {
        return this.queryRoomOrNull(roomId) ?: throw NotFoundException("Can not find room by id $roomId")
    }

    fun clearRoom(roomId: String) {
        //TODO
    }

    fun joinRoom(userId: String, room: String): Room {
        return queryRoomNotNull(room).let {
            synchronized(joinLock) {
                if (it.isFull) {
                    throw RuntimeException("room $room is full.")
                }
                if (!it.players.contains(userId)) {
                    it.players.add(userId)
                }
                it
            }
        }
    }
}