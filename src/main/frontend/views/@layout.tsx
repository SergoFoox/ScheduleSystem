import { AppLayout, DrawerToggle, Icon, Scroller, SideNav, SideNavItem, Checkbox } from '@vaadin/react-components';
import '@vaadin/icons/vaadin-iconset.js';
import { Suspense, useEffect } from 'react';
import { Outlet, useLocation } from 'react-router';
import { useSignal } from '@vaadin/hilla-react-signals';
import { ScheduleEndpoint } from '../generated/endpoints';
import { isPublished, refreshSchedule } from '../store/app-state';

export default function MainLayout() {
  const userRole = useSignal<string | undefined>(undefined);
  const isDispatcher = userRole.value === 'DISPATCHER'; 
  const location = useLocation();

  useEffect(() => {
    refreshSchedule(); // This updates global isPublished and scheduleData
    ScheduleEndpoint.getCurrentUserRole().then(role => userRole.value = role);
  }, []);

  useEffect(() => {
    window.dispatchEvent(new CustomEvent('side-nav-location-changed'));
  }, [location.pathname, location.search]);

  const handleToggle = async () => {
    await ScheduleEndpoint.togglePublishedStatus();
    await refreshSchedule();
  };

  return (
    <AppLayout primarySection="drawer">
      <div slot="navbar" className="flex items-center gap-4 px-4 w-full">
        <DrawerToggle aria-label="Перемкнути меню"></DrawerToggle>
        <h1 className="text-lg m-0">ASMS V3</h1>
        
        <div className="ml-auto flex items-center gap-4">
          <span className={`px-2 py-1 rounded text-sm font-bold`}
                style={{ 
                  backgroundColor: isPublished.value ? 'var(--aura-success-color)' : 'var(--aura-warning-color)',
                  color: 'white'
                }}>
            {isPublished.value ? 'ОПУБЛІКОВАНО' : 'ЧЕРНЕТКА'}
          </span>
          {isDispatcher && (
            <Checkbox 
              label="Опублікувати" 
              checked={isPublished.value} 
              onCheckedChanged={(e) => {
                if (e.detail.value !== isPublished.value) handleToggle();
              }} 
            />
          )}
        </div>
      </div>

      <Scroller slot="drawer" className="p-2">
        <SideNav>
          <SideNavItem path="/dashboard">
            <Icon icon="vaadin:calendar" slot="prefix" />
            Розклад
          </SideNavItem>
          <SideNavItem path="/teachers">
            <Icon icon="vaadin:user-card" slot="prefix" />
            Викладачі
          </SideNavItem>
          <SideNavItem path="/subjects">
            <Icon icon="vaadin:book" slot="prefix" />
            Дисципліни
          </SideNavItem>
          <SideNavItem path="/groups">
            <Icon icon="vaadin:users" slot="prefix" />
            Групи
          </SideNavItem>
          <SideNavItem path="/rooms">
            <Icon icon="vaadin:home" slot="prefix" />
            Аудиторії
          </SideNavItem>
          <SideNavItem path="/course-plans">
            <Icon icon="vaadin:list" slot="prefix" />
            Навчальні плани
          </SideNavItem>
        </SideNav>
      </Scroller>

      <Suspense fallback={<div>Завантаження...</div>}>
        <Outlet />
      </Suspense>
    </AppLayout>
  );
}
