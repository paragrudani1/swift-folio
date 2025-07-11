# Swift Folio

Swift Folio is a web application that allows you to manage your AWS S3 buckets with ease. It provides a user-friendly interface to explore your S3 buckets, upload and download files, and manage your storage.

## Features

- **Secure Authentication:** Securely connect to your AWS S3 account using your access key ID and secret access key. Your credentials can be saved securely in your browser's local storage for future use.
- **Bucket Exploration:** Explore the contents of your S3 buckets, including files and folders.
- **File Management:** Upload and download files to and from your S3 buckets. You can also delete files and create new folders.
- **Drag and Drop:** Easily upload files by dragging and dropping them into the application.
- **Progress Tracking:** Track the progress of your file uploads in real-time.
- **Storage Usage:** View the total storage usage of your S3 buckets.
- **Search and Sort:** Search for files and folders within your buckets and sort them by name, last modified date, or size.
- **Responsive Design:** The application is designed to be responsive and work on all devices.

## Getting Started

To get started with Swift Folio, you will need to have an AWS account and an S3 bucket. You will also need to have your access key ID and secret access key.

1.  Clone the repository:

`git clone https://github.com/your-username/swift-folio.git`

2.  Install the dependencies:

`npm install`

3.  Start the development server:

`npm run dev`

4.  Open your browser and navigate to `http://localhost:3000/s3` to access the application.

## Usage

1.  Enter your AWS access key ID, secret access key, and bucket name in the login form.
2.  Click the "Connect to S3" button to connect to your S3 bucket.
3.  Once you are connected, you will be able to explore the contents of your bucket.
4.  You can upload files by dragging and dropping them into the application or by clicking the "Upload Files" button.
5.  You can download files by clicking the "Download" button next to the file you want to download.
6.  You can delete files by clicking the "Delete" button next to the file you want to delete.
7.  You can create new folders by clicking the "New Folder" button.

## Technologies Used

- [Next.js](https://nextjs.org/) - A React framework for building server-side rendered and static web applications.
- [React](https://reactjs.org/) - A JavaScript library for building user interfaces.
- [Tailwind CSS](https://tailwindcss.com/) - A utility-first CSS framework for rapidly building custom designs.
- [AWS SDK for JavaScript](https://aws.amazon.com/sdk-for-javascript/) - A JavaScript library for interacting with AWS services.
- [React Dropzone](https://react-dropzone.js.org/) - A React component for creating a drag-and-drop file upload zone.
- [CryptoJS](https://cryptojs.gitbook.io/docs/) - A JavaScript library of crypto standards.

## Contributing

Contributions are welcome! If you have any ideas, suggestions, or bug reports, please open an issue or submit a pull request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more information.