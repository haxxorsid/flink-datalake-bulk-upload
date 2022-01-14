import axios from 'axios';

const api = axios.create({
    baseURL: '<fill-in>'
})

export function pushImageUploadEvents(images) {
    const data = new FormData()
    console.log(images);
    for(var x = 0; x<images.length; x++) {
        data.append('file', images[x])
    }
    return api
      .post('/push', data);
      
}

export function getImageUploadSummary() {
  return api
    .get('/summary');
    
}