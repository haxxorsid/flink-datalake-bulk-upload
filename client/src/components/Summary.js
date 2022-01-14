import React from 'react';

// Component to show GRID for upload summary
function Summary(props) {
    return (
        <div>
            <div className="row">
                    <div className="col themed-grid-col font-weight-bold">
                        Upload Summary Id
                    </div>
                    <div className="col themed-grid-col font-weight-bold">
                        File Name
                    </div>
                    <div className="col themed-grid-col font-weight-bold">
                        Number of same files in the same window
                    </div>
                    <div className="col themed-grid-col font-weight-bold">
                        Image URL from DataLake
                    </div>
                    <div className="col themed-grid-col font-weight-bold">
                        Created At (Window Time)
                    </div>
                </div>
            {props.uploads.map(record => 
                <div className="row" key={record.UploadSummaryId}>
                    <div className="col themed-grid-col">
                        {record.UploadSummaryId}
                    </div>
                    <div className="col themed-grid-col">
                        {record.FileName}
                    </div>
                    <div className="col themed-grid-col">
                        {record.Count}
                    </div>
                    <div className="col themed-grid-col">
                        <a href={record.Url}>Link</a>
                    </div>
                    <div className="col themed-grid-col">
                        {record.CreatedAt}
                    </div>
                </div>
            )}
        </div>
    )
}

export default Summary;