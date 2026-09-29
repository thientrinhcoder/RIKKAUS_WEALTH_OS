import { DESTINATIONS } from '@/components/shell/destinations';

describe('primary destinations', () => {
  it('has exactly the five approved destinations in the approved order', () => {
    expect(DESTINATIONS.map((destination) => destination.label)).toEqual([
      'Tổng quan',
      'Tài sản & Nợ',
      'Dòng tiền',
      'Mục tiêu',
      'Nhận định',
    ]);
  });

  it('gives every destination an icon and a route', () => {
    for (const destination of DESTINATIONS) {
      expect(destination.icon).toEqual(expect.any(String));
      expect(destination.route).toMatch(/^\//);
      expect(destination.key).toEqual(expect.any(String));
    }
  });

  it('uses a unique key and route per destination', () => {
    expect(new Set(DESTINATIONS.map((d) => d.key)).size).toBe(DESTINATIONS.length);
    expect(new Set(DESTINATIONS.map((d) => d.route)).size).toBe(DESTINATIONS.length);
  });

  it('keeps profile and settings out of primary navigation', () => {
    const labels = DESTINATIONS.map((destination) => destination.label);

    expect(labels).not.toContain('Hồ sơ');
    expect(labels).not.toContain('Cài đặt');
  });
});
