package com.sergofoox.domain.room;

import com.sergofoox.domain.ui.dto.RoomDTO;
import com.vaadin.hilla.BrowserCallable;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@BrowserCallable
@Service
@RolesAllowed("DISPATCHER")
public class RoomEndpoint {

    private final RoomRepository roomRepository;

    public RoomEndpoint(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<RoomDTO> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public void saveRoom(RoomDTO dto) {
        Room room;
        if (dto.id() != null) {
            room = roomRepository.findById(dto.id()).orElseThrow();
        } else {
            room = new Room();
        }
        
        room.setName(dto.name());
        room.setCapacity(dto.capacity());
        room.setBuilding(dto.building());
        room.setEquipment(dto.equipment());
        room.setType(dto.type());
        
        roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }

    private RoomDTO mapToDTO(Room room) {
        return new RoomDTO(
                room.getId(),
                room.getName(),
                room.getCapacity(),
                room.getBuilding(),
                room.getEquipment(),
                room.getType()
        );
    }
}
