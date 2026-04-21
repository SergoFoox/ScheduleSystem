import { ViewConfig } from '@vaadin/hilla-file-router/types.js';

export const config: ViewConfig = {
  title: 'Викладачі',
};

export default function TeachersView() {
  return (
    <div className="p-4">
      <h2 className="text-xl font-bold">Викладачі</h2>
      <p>Сторінка в розробці...</p>
    </div>
  );
}
