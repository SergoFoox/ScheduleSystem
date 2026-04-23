import React, { useEffect, useState } from 'react';
import { Grid } from '@vaadin/react-components/Grid.js';
import { GridColumn } from '@vaadin/react-components/GridColumn.js';
import { TextField } from '@vaadin/react-components/TextField.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { ConfirmDialog } from '@vaadin/react-components/ConfirmDialog.js';
import { RoomEndpoint } from '../generated/endpoints';
import type RoomDTO from '../generated/com/sergofoox/domain/ui/dto/RoomDTO';
import { RoomDialog } from '../components/RoomDialog';
import { useSignal } from '@vaadin/hilla-react-signals';

export default function RoomsView() {
  const [rooms, setRooms] = useState<RoomDTO[]>([]);
  const [filter, setFilter] = useState('');
  const [dialogOpened, setDialogOpened] = useState(false);
  const [confirmOpened, setConfirmOpened] = useState(false);
  const [selectedRoom, setSelectedRoom] = useState<RoomDTO | undefined>(undefined);
  const [roomToDelete, setRoomToDelete] = useState<number | undefined>(undefined);
  const loading = useSignal(true);

  const fetchRooms = async () => {
    loading.value = true;
    try {
      const data = await RoomEndpoint.getAllRooms();
      setRooms((data || []).filter(r => !!r) as RoomDTO[]);
    } catch (err) {
      console.error('Failed to fetch rooms:', err);
      Notification.show('Помилка завантаження аудиторій', { theme: 'error', position: 'bottom-end' });
    } finally {
      loading.value = false;
    }
  };

  useEffect(() => {
    fetchRooms();
  }, []);

  const filteredRooms = rooms.filter(room => 
    room.name?.toLowerCase().includes(filter.toLowerCase()) ||
    room.building?.toLowerCase().includes(filter.toLowerCase())
  );

  const handleAdd = () => {
    setSelectedRoom(undefined);
    setDialogOpened(true);
  };

  const handleEdit = (room: RoomDTO) => {
    setSelectedRoom(room);
    setDialogOpened(true);
  };

  const openDeleteConfirm = (id: number) => {
    setRoomToDelete(id);
    setConfirmOpened(true);
  };

  const handleDelete = async () => {
    if (roomToDelete === undefined) return;
    try {
      await RoomEndpoint.deleteRoom(roomToDelete as any);
      Notification.show('Аудиторію видалено', { theme: 'success', position: 'bottom-end' });
      setConfirmOpened(false);
      fetchRooms();
    } catch (err) {
      console.error('Failed to delete room:', err);
      Notification.show('Помилка при видаленні', { theme: 'error', position: 'bottom-end' });
    }
  };

  return (
    <div className="flex-1 flex flex-col gap-6 p-6 overflow-hidden bg-gray-50/50">
      <div className="flex justify-between items-center p-4 rounded-xl bg-white border shadow-sm">
        <div className="flex items-center gap-6 flex-1">
          <h2 className="text-2xl font-extrabold tracking-tight text-gray-900">
            Список аудиторій
          </h2>
          <TextField
            placeholder="Пошук за назвою або корпусом..."
            value={filter}
            onValueChanged={(e) => setFilter(e.detail.value)}
            className="w-96"
            clearButtonVisible
          >
            <Icon icon="vaadin:search" slot="prefix" className="text-gray-400" />
          </TextField>
        </div>
        <Button theme="primary" onClick={handleAdd} className="shadow-md">
          <Icon icon="vaadin:plus" slot="prefix" />
          Додати аудиторію
        </Button>
      </div>

      <div className="flex-1 overflow-hidden border rounded-xl shadow-lg bg-white">
        <Grid 
          items={filteredRooms} 
          className="h-full" 
          theme="row-stripes"
        >
          <GridColumn header="Назва" path="name" autoWidth />
          <GridColumn header="Корпус" path="building" autoWidth />
          <GridColumn header="Місткість" path="capacity" autoWidth textAlign="end" />
          <GridColumn 
            header="Тип" 
            autoWidth 
            renderer={({ item }) => (
              <span className="px-2 py-1 rounded-full text-xs font-medium bg-blue-50 text-blue-700 border border-blue-100">
                {item.type}
              </span>
            )}
          />
          <GridColumn header="Обладнання" path="equipment" flexGrow={1} />
          <GridColumn
            header={
              <div className="flex items-center gap-2">
                <Icon icon="vaadin:cog" className="w-3 h-3" />
                <span>Дії</span>
              </div>
            }
            autoWidth
            frozenToEnd
            renderer={({ item }) => (
              <div className="flex gap-2 p-1">
                <Button 
                  theme="tertiary icon" 
                  onClick={() => handleEdit(item as RoomDTO)}
                  title="Редагувати"
                >
                  <Icon icon="vaadin:edit" />
                </Button>
                <Button 
                  theme="tertiary error icon" 
                  onClick={(e) => {
                    e.stopPropagation();
                    if (item.id) openDeleteConfirm(item.id as any);
                  }}
                  title="Видалити"
                >
                  <Icon icon="vaadin:trash" />
                </Button>
              </div>
            )}
          />
        </Grid>
      </div>

      {dialogOpened && (
        <RoomDialog 
          opened={dialogOpened} 
          room={selectedRoom} 
          onClose={() => {
            setDialogOpened(false);
            setSelectedRoom(undefined);
          }}
          onSaved={fetchRooms}
        />
      )}

      <ConfirmDialog
        header="Видалення аудиторії"
        cancelButtonVisible
        confirmText="Видалити"
        cancelText="Скасувати"
        confirmTheme="error primary"
        opened={confirmOpened}
        onOpenedChanged={(e) => setConfirmOpened(e.detail.value)}
        onConfirm={handleDelete}
      >
        Ви впевнені, що хочете безповоротно видалити цю аудиторію?
      </ConfirmDialog>
    </div>
  );
}
