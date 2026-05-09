import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router';
import { Button } from '@vaadin/react-components/Button.js';
import { Grid } from '@vaadin/react-components/Grid.js';
import { GridColumn } from '@vaadin/react-components/GridColumn.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { Select } from '@vaadin/react-components/Select.js';
import { TextField } from '@vaadin/react-components/TextField.js';
import { CoursePlanEndpoint, GroupEndpoint } from '../generated/endpoints';
import type CoursePlanDTO from '../generated/com/sergofoox/domain/ui/dto/CoursePlanDTO';
import type GroupDTO from '../generated/com/sergofoox/domain/ui/dto/GroupDTO';
import { PlanDialog } from '../components/PlanDialog';
import { formatRoomType } from '../utils/labels';
import { BASE_TEMPLATE_LOCKED_MESSAGE, getMutationErrorMessage, isBaseTemplateLocked } from '../store/app-state';

export default function CoursePlansView() {
  const [plans, setPlans] = useState<CoursePlanDTO[]>([]);
  const [groups, setGroups] = useState<GroupDTO[]>([]);
  const [filter, setFilter] = useState('');
  const [expandedGroupId, setExpandedGroupId] = useState<number | undefined>(undefined);
  const [selectedPlan, setSelectedPlan] = useState<CoursePlanDTO | undefined>(undefined);
  const [defaultDialogGroupId, setDefaultDialogGroupId] = useState<number | undefined>(undefined);
  const [dialogOpened, setDialogOpened] = useState(false);
  const [copySourceGroupId, setCopySourceGroupId] = useState<number | undefined>(undefined);
  const [copying, setCopying] = useState(false);
  const [searchParams, setSearchParams] = useSearchParams();

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

  useEffect(() => {
    const groupId = searchParams.get('groupId');
    if (groupId) {
      setExpandedGroupId(parseInt(groupId, 10));
    } else {
      setExpandedGroupId(undefined);
    }
  }, [searchParams]);

  const plansByGroup = useMemo(() => {
    const grouped = new Map<number, CoursePlanDTO[]>();
    plans.forEach(plan => {
      if (!plan.groupId) return;
      const groupPlans = grouped.get(plan.groupId) || [];
      groupPlans.push(plan);
      grouped.set(plan.groupId, groupPlans);
    });
    return grouped;
  }, [plans]);

  const filteredGroups = groups.filter(group => {
    const search = filter.trim().toLowerCase();
    if (!search) return true;
    return group.name?.toLowerCase().includes(search) ||
      group.department?.toLowerCase().includes(search);
  });

  const getGroupPlans = (groupId?: number) => {
    if (!groupId) return [];
    return plansByGroup.get(groupId) || [];
  };

  const getTotalHours = (groupId?: number) => {
    return getGroupPlans(groupId).reduce((sum, plan) => sum + (plan.totalHours || 0), 0);
  };

  const handleToggleGroup = (group: GroupDTO) => {
    if (!group.id) return;
    const nextGroupId = expandedGroupId === group.id ? undefined : group.id;
    setExpandedGroupId(nextGroupId);
    setCopySourceGroupId(undefined);

    if (nextGroupId) {
      setSearchParams({ groupId: nextGroupId.toString() });
    } else {
      setSearchParams({});
    }
  };

  const handleAddPlan = (group: GroupDTO) => {
    if (isBaseTemplateLocked.value) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    setSelectedPlan(undefined);
    setDefaultDialogGroupId(group.id);
    setDialogOpened(true);
  };

  const handleEditPlan = (plan: CoursePlanDTO) => {
    if (isBaseTemplateLocked.value) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    setSelectedPlan(plan);
    setDefaultDialogGroupId(undefined);
    setDialogOpened(true);
  };

  const handleDelete = async (plan: CoursePlanDTO) => {
    if (isBaseTemplateLocked.value) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    if (!plan.id) return;
    if (!confirm(`Видалити "${plan.subjectName || 'дисципліну'}" з навчального плану?`)) return;

    try {
      await CoursePlanEndpoint.deletePlan(plan.id);
      Notification.show('План видалено', { theme: 'success' });
      await refreshData();
    } catch (err) {
      console.error(err);
      Notification.show(getMutationErrorMessage(err, 'Помилка видалення плану'), { theme: 'error' });
    }
  };

  const handleCopyPlans = async (targetGroup: GroupDTO) => {
    if (isBaseTemplateLocked.value) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    if (!targetGroup.id || !copySourceGroupId) {
      Notification.show('Оберіть групу-джерело', { theme: 'error' });
      return;
    }

    const sourceGroup = groups.find(g => g.id === copySourceGroupId);
    const confirmed = confirm(
      `Скопіювати навчальний план з групи "${sourceGroup?.name || ''}" до "${targetGroup.name}"?`
    );
    if (!confirmed) return;

    setCopying(true);
    try {
      const copiedCount = await CoursePlanEndpoint.copyPlansFromGroup(copySourceGroupId as any, targetGroup.id as any);
      Notification.show(
        copiedCount > 0 ? `Скопійовано дисциплін: ${copiedCount}` : 'Нових дисциплін для копіювання немає',
        { theme: copiedCount > 0 ? 'success' : 'primary' }
      );
      await refreshData();
    } catch (err) {
      console.error(err);
      Notification.show(getMutationErrorMessage(err, 'Помилка копіювання плану'), { theme: 'error' });
    } finally {
      setCopying(false);
    }
  };

  const closeDialog = () => {
    setDialogOpened(false);
    setSelectedPlan(undefined);
    setDefaultDialogGroupId(undefined);
  };

  return (
    <div className="flex-1 flex flex-col gap-4 p-6 overflow-hidden bg-gray-50/50">
      <div className="flex flex-wrap justify-between items-center gap-4 p-4 rounded-xl bg-white border shadow-sm shrink-0">
        <h2 className="text-2xl font-extrabold tracking-tight text-gray-900">
          Навчальні плани
        </h2>
        <TextField
          placeholder="Пошук групи або кафедри..."
          value={filter}
          onValueChanged={(e) => setFilter(e.detail.value)}
          className="w-96 max-w-full"
          clearButtonVisible
        >
          <Icon icon="vaadin:search" slot="prefix" className="text-gray-400" />
        </TextField>
      </div>

      <div className="flex-1 overflow-auto border rounded-xl bg-white shadow-sm">
        {filteredGroups.map(group => {
          const groupPlans = getGroupPlans(group.id);
          const expanded = expandedGroupId === group.id;
          const availableCopyGroups = groups.filter(g => g.id && g.id !== group.id);

          return (
            <div key={group.id} className="border-b last:border-b-0">
              <button
                type="button"
                className="w-full flex items-center gap-4 px-4 py-3 text-left hover:bg-gray-50"
                onClick={() => handleToggleGroup(group)}
              >
                <Icon icon={expanded ? 'vaadin:chevron-down' : 'vaadin:chevron-right'} className="w-4 h-4 text-gray-500" />
                <div className="min-w-0 flex-1">
                  <div className="font-semibold text-gray-900">{group.name}</div>
                  <div className="text-xs text-gray-500">{group.department}</div>
                </div>
                <div className="hidden sm:flex items-center gap-6 text-sm text-gray-600">
                  <span>Курс: {group.course}</span>
                  <span>Дисциплін: {groupPlans.length}</span>
                  <span>Годин: {getTotalHours(group.id)}</span>
                </div>
              </button>

              {expanded && (
                <div className="border-t bg-gray-50 px-4 py-4">
                  <div className="flex flex-wrap items-end gap-2 mb-4">
                    <Select
                      label="Скопіювати з групи"
                      className="min-w-[260px]"
                      items={availableCopyGroups.map(g => ({ label: g.name, value: g.id?.toString() }))}
                      value={copySourceGroupId?.toString()}
                      onValueChanged={(e) => setCopySourceGroupId(e.detail.value ? parseInt(e.detail.value, 10) : undefined)}
                      disabled={copying}
                    />
                    <Button
                      theme="secondary"
                      onClick={() => handleCopyPlans(group)}
                      disabled={!copySourceGroupId || copying}
                      title="Скопіювати план з вибраної групи"
                    >
                      <Icon icon="vaadin:copy" slot="prefix" />
                      {copying ? 'Копіювання...' : 'Скопіювати'}
                    </Button>
                    <Button theme="primary" onClick={() => handleAddPlan(group)}>
                      <Icon icon="vaadin:plus" slot="prefix" />
                      Додати дисципліну
                    </Button>
                  </div>

                  {groupPlans.length > 0 ? (
                    <Grid items={groupPlans} className="border rounded-lg bg-white" theme="row-stripes" allRowsVisible>
                      <GridColumn path="subjectName" header="Дисципліна" flexGrow={1} />
                      <GridColumn
                        header="Викладачі"
                        autoWidth
                        renderer={({ item }) => (
                          <span>{[item.teacherName, item.secondTeacherName].filter(Boolean).join(', ')}</span>
                        )}
                      />
                      <GridColumn path="lectureHours" header="Лекції" autoWidth textAlign="end" />
                      <GridColumn path="practiceHours" header="Практ." autoWidth textAlign="end" />
                      <GridColumn path="labHours" header="Лаб." autoWidth textAlign="end" />
                      <GridColumn path="totalHours" header="Всього" autoWidth textAlign="end" />
                      <GridColumn
                        header="Аудиторія"
                        autoWidth
                        renderer={({ item }) => (
                          <span>{formatRoomType((item as CoursePlanDTO).requiredRoomType)}</span>
                        )}
                      />
                      <GridColumn
                        header="Дії"
                        autoWidth
                        frozenToEnd
                        renderer={({ item }) => (
                          <div className="flex gap-2 p-1">
                            <Button
                              theme="tertiary icon"
                              onClick={() => handleEditPlan(item as CoursePlanDTO)}
                              title="Редагувати"
                            >
                              <Icon icon="vaadin:edit" />
                            </Button>
                            <Button
                              theme="tertiary error icon"
                              onClick={() => handleDelete(item as CoursePlanDTO)}
                              title="Видалити"
                            >
                              <Icon icon="vaadin:trash" />
                            </Button>
                          </div>
                        )}
                      />
                    </Grid>
                  ) : (
                    <div className="rounded-lg border bg-white px-4 py-6 text-sm text-gray-500">
                      План для цієї групи ще порожній.
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {dialogOpened && (
        <PlanDialog
          opened={dialogOpened}
          plan={selectedPlan}
          defaultGroupId={defaultDialogGroupId}
          onClose={closeDialog}
          onSaved={async () => {
            closeDialog();
            await refreshData();
          }}
        />
      )}
    </div>
  );
}
