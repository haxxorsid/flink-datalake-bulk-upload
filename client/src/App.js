import './App.css';
import 'react-toastify/dist/ReactToastify.css';

import { useEffect, useState } from 'react';
import {pushImageUploadEvents, getImageUploadSummary} from './common/api';
import ImageForm from './components/ImageForm';
import Summary from './components/Summary';
import { ToastContainer, toast } from 'react-toastify';

function App() {
  const [selectedImages, setSelectedImages] = useState("");
  const [uploadSummary, setUploadSummary] = useState([]);
  const [errorMessage, setErrorMessage] = useState("");

  // Fetch upload summary every 10 seconds
  useEffect(() => {
    fetchSummary();
    const interval = setInterval(() => {
      fetchSummary();
    }, 10000);
    return () => clearInterval(interval);
  }, []);

  // Fetch upload summary
  function fetchSummary() {
    getImageUploadSummary().then(res => {

      setUploadSummary(res.data);
    }).catch(error => {
      toast.error('Some error occured in fetching summary!');
      console.log(error);
    });
  }

  // Send images to node server
  function onClickHandler() {
    //check if more than 0 images and there is no size error (more than 300kb)
    if(selectedImages.length > 0 && !(errorMessage)) {
      pushImageUploadEvents(selectedImages).then(res => {
        toast.success('Images sent to Apache kafka');
        console.log(res.data);
      }).catch(error => {
        toast.error('Some error occured in uploading images!');
        console.log(error);
      });
    }
  }

  // check if size of all images is less than 300kb, else show error
  function checkImages(images) {
    for(var x=0;x< images.length;x++) {
      if(images[x].size/1000 > 300) {
        toast.error('One of the selected images has size above 300kb size');
        setErrorMessage("Please select an image below 300kb size.");
        return;
      }
    }
    setErrorMessage(null);
    setSelectedImages(images);
  }

  return (
    <div className="App">
      <ToastContainer/>
      <div className="container">
        <div className="py-5 text-center">
          <h2>Bulk Streaming upload of Images with Apache Flink, Kafka, Data Lake, and Kubernetes.</h2>
        </div>
        <ImageForm onClickHandler={onClickHandler} onInputChange={checkImages} />
        <div className="text-danger fs-6">
          {errorMessage}
        </div>
        <p className="col-lg-12 fs-4">
          Wait for about 30 seconds to see your uploaded images in the below table. This grid below automatically refreshes every 10 seconds.
        </p>
        
        <Summary uploads={uploadSummary} />
        </div>
      </div>
  );
}

export default App;
