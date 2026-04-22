import React, { useEffect, useState } from 'react';
import { Grid } from '@vaadin/react-components/Grid.js';
import { GridColumn } from '@vaadin/react-components/GridColumn.js';
import { TextField } from '@vaadin/react-components/TextField.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { ConfirmDialog } from '@vaadin/react-components/ConfirmDialog.js';
import { GroupEndpoint } from '../generated/endpoints';
import type GroupDTO from '../generated/com/sergofoox/domain/ui/dto/GroupDTO';
import { GroupDialog } from '../components/GroupDialog';
import { useSignal } from '@vaadin/hilla-react-signals';

export default function GroupsView() {
  const [groups, setGroups] = useState<GroupDTO[]>([]);
  const [filter, setFilter] = useState('');
  const [dialogOpened, setDialogOpened] = useState(false);
  const [confirmOpened, setConfirmOpened] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState<GroupDTO | undefined>(undefined);
  const [groupToDelete, setGroupToDelete] = useState<number | undefined>(undefined);
  const loading = useSignal(true);

  const fetchGroups = async () => {
    loading.value = true;
    try {
      const data = await GroupEndpoint.getAllGroups();
      setGroups((data || []).filter(g => !!g) as GroupDTO[]);
    } catch (err) {
      console.error('Failed to fetch groups:', err);
      Notification.show('Помилка завантаження груп', { theme: 'error', position: 'bottom-end' });
    } finally {
      loading.value = false;
    }
  };

  useEffect(() => {
    fetchGroups();
  }, []);

  const filteredGroups = groups.filter(group => 
    group.name?.toLowerCase().includes(filter.toLowerCase()) ||
    group.department?.toLowerCase().includes(filter.toLowerCase())
  );

  const handleAdd = () => {
    setSelectedGroup(undefined);
    setDialogOpened(true);
  };

  const handleEdit = (group: GroupDTO) => {
    setSelectedGroup(group);
    setDialogOpened(true);
  };

  const openDeleteConfirm = (id: number) => {
    setGroupToDelete(id);
    setConfirmOpened(true);
  };

  const handleDelete = async () => {
    if (groupToDelete === undefined) return;
    try {
      await GroupEndpoint.deleteGroup(groupToDelete as any);
      Notification.show('Групу видалено', { theme: 'success', position: 'bottom-end' });
      setConfirmOpened(false);
      fetchGroups();
    } catch (err) {
      console.error('Failed to delete group:', err);
      Notification.show('Помилка при видаленні', { theme: 'error', position: 'bottom-end' });
    }
  };

  return (
    <div className="flex-1 flex flex-col gap-6 p-6 overflow-hidden bg-gray-50/50">
      <div className="flex justify-between items-center p-4 rounded-xl bg-white border shadow-sm">
        <div className="flex items-center gap-6 flex-1">
          <h2 className="text-2xl font-extrabold tracking-tight text-gray-900">
            Список груп
          </h2>
          <TextField
            placeholder="Пошук за назвою або кафедрою..."
            value={filter}
            onValueChanged={(e) => setFilter(e.detail.value)}
            className="w-96"
            clearButtonVisible
          >
            <Icon icon="lumo:search" slot="prefix" className="text-gray-400" />
          </TextField>
        </div>
        <Button theme="primary" onClick={handleAdd} className="shadow-md">
          <Icon icon="lumo:plus" slot="prefix" />
          Додати групу
        </Button>
      </div>

      <div className="flex-1 overflow-hidden border rounded-xl shadow-lg bg-white">
        <Grid 
          items={filteredGroups} 
          className="h-full" 
          theme="row-stripes"
          onActiveItemChanged={(e) => {
            const item = e.detail.value;
            if (item) handleEdit(item as GroupDTO);
          }}
        >
          <GridColumn header="Назва групи" path="name" autoWidth />
          <GridColumn header="Курс" path="course" autoWidth textAlign="center" />
          <GridColumn header="Кількість студентів" path="size" autoWidth textAlign="end" />
          <GridColumn header="Кафедра" path="department" flexGrow={1} />
          <GridColumn
            header="Дії"
            autoWidth
            frozenToEnd
            renderer={({ item }) => (
              <div className="flex gap-2 p-1">
                <Button 
                  theme="tertiary error icon" 
                  onClick={(e) => {
                    e.stopPropagation();
                    if (item.id) openDeleteConfirm(item.id as any);
                  }}
                  title="Видалити"
                >
                  <Icon icon="lumo:trash" />
                </Button>
              </div>
            )}
          />
        </Grid>
      </div>

      <GroupDialog 
        opened={dialogOpened} 
        group={selectedGroup} 
        onClose={() => setDialogOpened(false)}
        onSaved={fetchGroups}
      />

      <ConfirmDialog
        header="Видалення групи"
        cancelButtonVisible
        confirmText="Видалити"
        cancelText="Скасувати"
        confirmTheme="error primary"
        opened={confirmOpened}
        onOpenedChanged={(e) => setConfirmOpened(e.detail.value)}
        onConfirm={handleDelete}
      >
        Ви впевнені, що хочете видалити цю групу? Це може вплинути на існуючий розклад.
      </ConfirmDialog>
    </div>
  );
}
