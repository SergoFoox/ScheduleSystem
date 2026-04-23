import { useEffect, useState } from 'react';
import { Button } from '@vaadin/react-components/Button.js';
import { Grid } from '@vaadin/react-components/Grid.js';
import { GridColumn } from '@vaadin/react-components/GridColumn.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { CoursePlanEndpoint, GroupEndpoint } from '../generated/endpoints';
import type CoursePlanDTO from '../generated/com/sergofoox/domain/ui/dto/CoursePlanDTO';
import type GroupDTO from '../generated/com/sergofoox/domain/ui/dto/GroupDTO';
import { PlanDialog } from '../components/PlanDialog';

export default function CoursePlansView() {
  const [plans, setPlans] = useState<CoursePlanDTO[]>([]);
  const [groups, setGroups] = useState<GroupDTO[]>([]);
  const [selectedPlan, setSelectedPlan] = useState<CoursePlanDTO | undefined>(undefined);
  const [dialogOpened, setDialogOpened] = useState(false);

  const refreshData = async () => {
    try {
      const [plansData, groupsData] = await Promise.all([
        CoursePlanEndpoint.getAllPlans(),
        GroupEndpoint.getAllGroups()
      ]);
      setPlans((plansData || []).filter(p => !!p) as CoursePlanDTO[]);
      setGroups((groupsData || []).filter(g => !!g) as GroupDTO[]);
    } catch (err) {
      console.error(err);
      Notification.show('Помилка завантаження даних', { theme: 'error' });
    }
  };

  useEffect(() => {
    refreshData();
  }, []);

  const handleEdit = (plan: CoursePlanDTO) => {
    setSelectedPlan(plan);
    setDialogOpened(true);
  };

  const handleDelete = async (id: number) => {
    if (confirm('Ви впевнені, що хочете видалити цей план навантаження?')) {
      try {
        await CoursePlanEndpoint.deletePlan(id);
        Notification.show('План видалено', { theme: 'success' });
        refreshData();
      } catch (err) {
        console.error(err);
        Notification.show('Помилка видалення плану', { theme: 'error' });
      }
    }
  };

  const getGroupName = (groupId: number) => {
    return groups.find(g => g.id === groupId)?.name || 'Невідома група';
  };

  return (
    <div className="p-m flex flex-col gap-m h-full overflow-hidden">
      <div className="flex justify-between items-center px-4 pt-4 shrink-0">
        <h2 className="text-xl font-bold">Навчальні плани (Навантаження)</h2>
        <Button theme="primary" onClick={() => { setSelectedPlan(undefined); setDialogOpened(true); }}>
          <Icon icon="vaadin:plus" slot="prefix" />
          Додати навантаження
        </Button>
      </div>

      <div className="flex-grow overflow-hidden px-4 mb-4">
        <Grid 
          items={plans} 
          className="h-full border rounded-lg shadow-sm"
          onActiveItemChanged={(e) => {
            const item = e.detail.value;
            if (item) handleEdit(item as CoursePlanDTO);
          }}
        >
          <GridColumn 
            header="Група" 
            renderer={({ item }) => <span>{getGroupName(item.groupId)}</span>} 
            autoWidth 
          />
          <GridColumn path="subjectName" header="Дисципліна" flexGrow={1} />
          <GridColumn path="totalHours" header="Всього год." autoWidth textAlign="end" />
          <GridColumn path="lectureHours" header="Лекції" autoWidth textAlign="end" />
          <GridColumn path="practiceHours" header="Практ." autoWidth textAlign="end" />
          <GridColumn path="labHours" header="Лаб." autoWidth textAlign="end" />
          <GridColumn
            header="Дії"
            autoWidth
            frozenToEnd
            renderer={({ item }) => (
              <Button
                theme="error icon"
                onClick={(e) => {
                  e.stopPropagation();
                  handleDelete(item.id!);
                }}
              >
                <Icon icon="vaadin:trash" />
              </Button>
            )}
          />
        </Grid>
      </div>

      {dialogOpened && (
        <PlanDialog
          opened={dialogOpened}
          plan={selectedPlan}
          onClose={() => setDialogOpened(false)}
          onSaved={() => {
            setDialogOpened(false);
            refreshData();
          }}
        />
      )}
    </div>
  );
}
