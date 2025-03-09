import React, {useEffect, useState} from 'react';
import withScreenSecurity from "../../../withScreenSecurity";
import {
  Button,
  Dialog,
  DialogContent,
  DialogTitle,
  Input
} from "@material-ui/core";
import FilePreviewUrl from "../../../common/uploader/FilePreviewUrl";
import {DialogActions} from "@mui/material";
import {getFilenameFromString, getFilePathFromString} from "../ultils/FileUltils";
import CustomizedDialogs from "../../../dialog/CustomizedDialogs";
import {makeStyles} from "@material-ui/core/styles";
import TertiaryButton from "../../../button/TertiaryButton";
import PrimaryButton from "../../../button/PrimaryButton";
import ImageEditor from "../../../common/uploader/ImageEditor";
import SecondaryButton from "../../../button/SecondaryButton";

const useStyles = makeStyles((theme) => ({
  dialogContent: {minWidth: '90vw'},
}));

function QuestionFilePreview(props) {
  const classes = useStyles();
  const {
    open,
    setOpen,
    file,
    examResultDetailsIdSelected,
    isComment,
    imageComment,
    setImageComment,
  } = props;

  const [isEdit, setIsEdit] = useState(false)
  const [isSave, setIsSave] = useState(false)

  useEffect(() => {
    if(imageComment){
      closeDialog()
    }
  }, [imageComment]);

  const closeDialog = () => {
    setOpen(false)
    setIsEdit(false)
    setIsSave(false)
  }

  const handleDownload = () => {
    fetch(file)
      .then(response => response.blob())
      .then(blob => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = getFilenameFromString(file); // Tên tệp tải xuống
        link.click();
        window.URL.revokeObjectURL(url); // Dọn dẹp URL blob
      })
      .catch(error => console.error('Error downloading the file:', error));
  }

  return (
    <div>
      <CustomizedDialogs
        open={open}
        handleClose={closeDialog}
        classNames={{paper: classes.dialogContent}}
        content={
          <div>
            {/*<FilePreviewUrl file={file}></FilePreviewUrl>*/}
            <ImageEditor
              file={file}
              examResultDetailsIdSelected={examResultDetailsIdSelected}
              isEdit={isEdit}
              isSave={isSave}
              setImageComment={setImageComment}
            />
          </div>
        }
        actions={
          <div style={{display: "flex", justifyContent: "center", alignItems: "center", gap: "15px"}}>
            <TertiaryButton
              variant="outlined"
              onClick={closeDialog}
            >
              Hủy
            </TertiaryButton>
            {
              !isEdit && (
                <PrimaryButton
                  variant="contained"
                  color="primary"
                  onClick={handleDownload}
                >
                  Tải xuống
                </PrimaryButton>
              )
            }
            {
              isComment && !isEdit && (
                <SecondaryButton
                  variant="outlined"
                  onClick={() => setIsEdit(true)}
                >
                  Nhận xét
                </SecondaryButton>
              )
            }
            {
              isComment && isEdit && (
                <PrimaryButton
                  variant="contained"
                  color="primary"
                  onClick={() => setIsSave(true)}
                >
                  Lưu
                </PrimaryButton>
              )
            }
          </div>
        }
      />
    </div>
  );
}

const screenName = "MENU_EXAM_QUESTION_BANK";
// export default withScreenSecurity(QuestionBank, screenName, true);
export default QuestionFilePreview;
