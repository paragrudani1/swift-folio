import React from 'react';
import { S3Explorer } from '../s3/page';
import { Dropzone } from '../s3/Dropzone';
import { ThemeProvider } from '../contexts/ThemeContext';

export default {
  title: 'Components/S3Explorer',
  component: S3Explorer,
};

const Template = (args) => (
  <ThemeProvider>
    <S3Explorer {...args} />
  </ThemeProvider>
);

export const Default = Template.bind({});
Default.args = {
  // Add any needed prop arguments here
};

export const DropzoneExample = () => (
  <ThemeProvider>
    <Dropzone 
      s3Client={null}
      bucketName=""
      prefix=""
      onUploadSuccess={() => {}}
      onProgress={() => {}}
    />
  </ThemeProvider>
);
