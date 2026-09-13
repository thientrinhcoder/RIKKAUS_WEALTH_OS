import { getResponsiveLayout } from '@/ui/responsive';

describe('responsive layout', () => {
  it('uses mobile padding at 375px', () => {
    expect(getResponsiveLayout(375)).toEqual({ horizontalPadding: 16 });
  });

  it('uses wide padding at 1024px', () => {
    expect(getResponsiveLayout(1024)).toEqual({ horizontalPadding: 24 });
  });
});
