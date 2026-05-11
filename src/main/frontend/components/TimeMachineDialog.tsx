import React, { useEffect, useState } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { HorizontalLayout } from '@vaadin/react-components/HorizontalLayout.js';
import { VerticalLayout } from '@vaadin/react-components/VerticalLayout.js';
import { Checkbox } from '@vaadin/react-components/Checkbox.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { ConfirmDialog } from '@vaadin/react-components/ConfirmDialog.js';
import { AutosaveEndpoint } from '../generated/endpoints';
import type AutosaveSnapshotDTO from '../generated/com/sergofoox/domain/autosave/AutosaveSnapshotDTO';
import { refreshSchedule } from '../store/app-state';

interface TimeMachineDialogProps {
  opened: boolean;
  onClose: () => void;
}

export const TimeMachineDialog: React.FC<TimeMachineDialogProps> = ({ opened, onClose }) => {
  const [snapshots, setSnapshots] = useState<AutosaveSnapshotDTO[]>([]);
  const [selectedSnapshot, setSelectedSnapshot] = useState<AutosaveSnapshotDTO | null>(null);
  const [confirmOpened, setConfirmOpened] = useState(false);
  const [asNewTemplate, setAsNewTemplate] = useState(true);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (opened) {
      fetchSnapshots();
    }
  }, [opened]);

  const fetchSnapshots = async () => {
    try {
      const data = await AutosaveEndpoint.getLatestSnapshots();
      // Фільтруємо undefined для відповідності типу AutosaveSnapshotDTO[]
      const validSnapshots = (data || []).filter((s): s is AutosaveSnapshotDTO => !!s);
      setSnapshots(validSnapshots);
    } catch (err) {
      console.error('Failed to fetch snapshots:', err);
      Notification.show('Помилка завантаження історії', { theme: 'error' });
    }
  };

  const handleRestore = async () => {
    if (!selectedSnapshot) return;
    setLoading(true);
    try {
      await AutosaveEndpoint.restoreSnapshot(selectedSnapshot.id, asNewTemplate);
      Notification.show('Розклад успішно відновлено', { theme: 'success' });
      await refreshSchedule();
      setConfirmOpened(false);
      onClose();
    } catch (err) {
      console.error('Failed to restore snapshot:', err);
      Notification.show('Помилка при відновленні розкладу', { theme: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (event: React.MouseEvent, id: number) => {
    event.stopPropagation();
    if (!window.confirm('Ви впевнені, що хочете видалити цей знімок?')) return;
    
    try {
      await AutosaveEndpoint.deleteSnapshot(id);
      Notification.show('Знімок видалено', { theme: 'success' });
      fetchSnapshots();
    } catch (err) {
      console.error('Failed to delete snapshot:', err);
      Notification.show('Помилка при видаленні знімка', { theme: 'error' });
    }
  };

  return (
    <>
      <Dialog
        headerTitle="Машина часу (Автозбереження)"
        opened={opened}
        onOpenedChanged={(e) => !e.detail.value && onClose()}
        footerRenderer={() => (
          <Button onClick={onClose}>Закрити</Button>
        )}
      >
        <div className="w-[450px] max-w-full">
          {snapshots.length === 0 ? (
            <div className="flex flex-col items-center justify-center p-8 text-gray-400">
              <Icon icon="vaadin:time-backward" className="w-12 h-12 mb-2 opacity-20" />
              <span>Історія автозбережень порожня</span>
            </div>
          ) : (
            <div className="flex flex-col gap-2 p-2 max-h-[60vh] overflow-y-auto">
              {snapshots.map((snapshot) => (
                <div
                  key={snapshot.id}
                  className="flex items-center justify-between p-4 bg-white border border-gray-100 rounded-xl shadow-sm hover:border-blue-300 hover:shadow-md transition-all cursor-pointer group"
                  onClick={() => {
                    setSelectedSnapshot(snapshot);
                    setConfirmOpened(true);
                  }}
                >
                  <div className="flex items-center gap-4">
                    <div className={`p-2 rounded-lg transition-colors ${snapshot.isManual ? 'bg-amber-50 text-amber-600 group-hover:bg-amber-600 group-hover:text-white' : 'bg-blue-50 text-blue-600 group-hover:bg-blue-600 group-hover:text-white'}`}>
                      <Icon icon={snapshot.isManual ? "vaadin:user-check" : "vaadin:clock"} className="w-5 h-5" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <div className="font-bold text-gray-900">{snapshot.timestamp}</div>
                        {snapshot.isManual && (
                          <span className="px-1.5 py-0.5 bg-amber-100 text-amber-700 text-[10px] font-black uppercase rounded tracking-wider">
                            Ручне
                          </span>
                        )}
                      </div>
                      <div className="text-xs text-gray-500">
                        Об'єктів у знімку: <span className="font-semibold text-gray-700">{snapshot.entityCount}</span>
                      </div>
                    </div>
                  </div>
                  
                  <div className="flex items-center gap-2">
                    <Button
                      theme="tertiary-inline error"
                      className="opacity-0 group-hover:opacity-100 transition-opacity p-2"
                      onClick={(e) => handleDelete(e, snapshot.id!)}
                      title="Видалити знімок"
                    >
                      <Icon icon="vaadin:trash" className="w-4 h-4" />
                    </Button>
                    <Icon icon="vaadin:chevron-right" className="text-gray-300 group-hover:text-blue-500 transition-colors" />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </Dialog>

      <ConfirmDialog
        header="Відновлення розкладу"
        cancelButtonVisible
        confirmText="Відновити"
        cancelText="Скасувати"
        confirmTheme="primary"
        opened={confirmOpened}
        onOpenedChanged={(e) => setConfirmOpened(e.detail.value)}
        onConfirm={handleRestore}
      >
        <VerticalLayout theme="spacing">
          <p>Ви впевнені, що хочете відновити розклад від <strong>{selectedSnapshot?.timestamp}</strong>?</p>
          <Checkbox
            label="Створити новий розклад як копію"
            checked={asNewTemplate}
            onCheckedChanged={(e) => setAsNewTemplate(e.detail.value)}
          />
          <p className="text-xs text-gray-500 italic">
            {asNewTemplate 
              ? "Поточний розклад залишиться незмінним. Буде створено новий збережений розклад." 
              : "УВАГА: Поточний розклад буде ПЕРЕЗАПИСАНО даними з цього знімка."}
          </p>
        </VerticalLayout>
      </ConfirmDialog>
    </>
  );
};
