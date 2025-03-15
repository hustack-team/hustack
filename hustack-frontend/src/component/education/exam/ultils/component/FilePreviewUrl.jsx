import React, {useEffect, useMemo, useState} from "react";
import { makeStyles } from "@material-ui/core/styles";
import {CircularProgress} from "@mui/material";
import {checkFilePdf} from "../FileUltils";

const useStyles = makeStyles((theme) => ({
  filePreview: {
    border: "solid gray 1px",
    borderRadius: "5px",
    height: "85vh",
    width: '100%',
    objectFit: 'contain'
  },
}));

export default function FilePreviewUrl(props) {
  const classes = useStyles();
  const [isLoading, setIsLoading] = useState(true);
  const file = useMemo(
    () => ({
      src: props.file
    }),
    [props.file]
  );
  useEffect(() => {
    if(!checkFilePdf(file.src)){
      setIsLoading(false)
    }
  }, [file.src]);
  return (
    <div style={{width: "100%", height: "100%"}}>
      {isLoading && (
        <div
          style={{
            position: "absolute",
            top: "50%",
            left: "50%",
            transform: "translate(-50%, -50%)",
          }}
        >
          <CircularProgress size={40}/>
        </div>
      )}
      {
        checkFilePdf(file.src) ? (
          <embed
            src={file.src}
            className={classes.filePreview}
            onLoad={() => setIsLoading(false)}
            {...props}
          />
        ) : (
          <div>
            <h3 style={{textAlign: "center"}}>Nhấn nút Tải xuống để tải về xem chi tiết!</h3>
          </div>
        )
      }

    </div>
  );
}
