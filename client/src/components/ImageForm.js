import React from 'react';

function ImageForm(props) {
    return (
        <div className="row">
          <div className="mb-3">
            <label htmlFor="images" className="form-label">Upload Images (below 300Kb)</label>
            <input onChange={(e) => props.onInputChange(e.target.files)} className="form-control" type="file" id="images" accept="image/*" multiple/>
          </div>
          <button type="button" className="btn btn-primary" onClick={props.onClickHandler}>Upload</button>
        </div>
    )
}

export default ImageForm;