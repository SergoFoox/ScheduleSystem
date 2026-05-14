import React, { useEffect, useState } from 'react';
import { Grid } from '@vaadin/react-components/Grid.js';
import { GridColumn } from '@vaadin/react-components/GridColumn.js';
import { TextField } from '@vaadin/react-components/TextField.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { GroupEndpoint } from '../generated/endpoints';
import type GroupDTO from '../generated/com/sergofoox/domain/ui/dto/GroupDTO';
import { GroupDialog } from '../components/GroupDialog';
import { useSignal } from '@vaadin/hilla-react-signals';
import { useNavigate } from 'react-router';
import { BASE_TEMPLATE_LOCKED_MESSAGE, isBaseTemplateLocked } from '../store/app-state';
import { useCrossTabRefresh } from '../utils/cross-tab-sync';

export default function GroupsView() {
  const [groups, setGroups] = useState<GroupDTO[]>([]);
  const [filter, setFilter] = useState('');
  const [dialogOpened, setDialogOpened] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState<GroupDTO | undefined>(undefined);
  const [activeItem, setActiveItem] = useState<GroupDTO | null>(null);
  const loading = useSignal(true);
  const navigate = useNavigate();

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

  useCrossTabRefresh(() => fetchGroups());

  const filteredGroups = groups.filter(group => 
    group.name?.toLowerCase().includes(filter.toLowerCase()) ||
    group.department?.toLowerCase().includes(filter.toLowerCase())
  );

  const handleAdd = () => {
    if (isBaseTemplateLocked.value) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    setSelectedGroup(undefined);
    setActiveItem(null);
    setDialogOpened(true);
  };

  const handleEdit = (group: GroupDTO) => {
    if (isBaseTemplateLocked.value) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    setSelectedGroup(group);
    setActiveItem(group);
    setDialogOpened(true);
  };

  const handleCloseDialog = () => {
    setDialogOpened(false);
    setActiveItem(null);
    setSelectedGroup(undefined);
  };

  const handleOpenPlans = (group: GroupDTO) => {
    if (!group.id) return;
    navigate(`/course-plans?groupId=${group.id}`);
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
            <Icon icon="vaadin:search" slot="prefix" className="text-gray-400" />
          </TextField>
        </div>
        <Button theme="primary" onClick={handleAdd} className="shadow-md">
          <Icon icon="vaadin:plus" slot="prefix" />
          Додати групу
        </Button>
      </div>

      <div className="flex-1 overflow-hidden border rounded-xl shadow-lg bg-white">
        <Grid 
          items={filteredGroups} 
          className="h-full" 
          theme="row-stripes"
        >
          <GridColumn header="Назва групи" path="name" autoWidth />
          <GridColumn header="Курс" path="course" autoWidth textAlign="center" />
          <GridColumn header="Кількість студентів" path="size" autoWidth textAlign="end" />
          <GridColumn header="Кафедра" path="department" flexGrow={1} />
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
                  onClick={() => handleEdit(item as GroupDTO)}
                  title="Редагувати"
                >
                  <Icon icon="vaadin:edit" />
                </Button>
                <Button 
                  theme="tertiary icon" 
                  onClick={() => handleOpenPlans(item as GroupDTO)}
                  title="Навчальні плани"
                >
                  <Icon icon="vaadin:list" />
                </Button>
              </div>
            )}
          />
        </Grid>
      </div>

      {dialogOpened && (
        <GroupDialog 
          opened={dialogOpened} 
          group={selectedGroup} 
          onClose={handleCloseDialog}
          onSaved={fetchGroups}
        />
      )}
    </div>
  );
}
