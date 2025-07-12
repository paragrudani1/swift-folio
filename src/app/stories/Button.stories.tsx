import React from 'react';
import { withThemes } from '@react-theming/storybook-addon';
import { themes } from '../theme/tokens';

export default {
  title: 'Form Elements/Button',
  decorators: [withThemes(null, themes)],
};

export const DefaultButton = () => (
  <button className="bg-surface hover:bg-surface-hover text-primary">
    Theme Aware Button
  </button>
);
