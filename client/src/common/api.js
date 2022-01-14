import axios from 'axios';

// api object for hitting node server
const api = axios.create({
    baseURL: '<fill-in>'
})

// function to post images in a batch on node server
export function pushImageUploadEvents(images) {
    const data = new FormData()
    console.log(images);
    for(var x = 0; x<images.length; x++) {
        data.append('file', images[x])
    }
    return api
      .post('/push', data);
      
}

// function to get upload summary from node server by GET (this executes once every 10 seconds)
export function getImageUploadSummary() {
  return api
    .get('/summary');
    
}