import React, {useState} from 'react';
import {getFilenameFromString, getFilePathFromString} from "../ultils/FileUltils";
import {AttachFileOutlined} from "@mui/icons-material";
import QuestionFilePreview from "./QuestionFilePreview";
import {parseHTMLToString} from "../ultils/DataUltils";
import CustomizedDialogs from "../../../dialog/CustomizedDialogs";
import {makeStyles} from "@material-ui/core/styles";
import TertiaryButton from "../../../button/TertiaryButton";

const useStyles = makeStyles((theme) => ({
  dialogContent: {minWidth: '70vw'},
}));

function QuestionBankDetails(props) {
  const classes = useStyles();
  const { open, setOpen, question} = props;

  const [openFilePreviewDialog, setOpenFilePreviewDialog] = useState(false);
  const [filePreview, setFilePreview] = useState(null);

  const closeDialog = () => {
    setOpen(false)
  }

  const handleOpenFilePreviewDialog = (data) => {
    setOpenFilePreviewDialog(true)
    setFilePreview(getFilePathFromString(data))
  };

  return (
    <div>
      <CustomizedDialogs
        open={open}
        classNames={{paper: classes.dialogContent}}
        handleClose={closeDialog}
        title={`Chi tiết câu hỏi - ${question?.code}`}
        content={
          <div>
            <div style={{display: 'flex', alignItems: 'center', marginBottom: '18px'}}>
              {
                question?.level === "EASY" && (
                  <strong style={{
                    color: "#61bd6d",
                    padding: '5px 10px',
                    border: '1px solid #61bd6d',
                    borderRadius: '20px'
                  }}>Dễ</strong>
                )
              }
              {
                question?.level === "MEDIUM" && (
                  <strong
                    style={{color: '#716DF2', padding: '5px 10px', border: '1px solid #716DF2', borderRadius: '20px'}}>Trung
                    bình</strong>
                )
              }
              {
                question?.level === "HARD" && (
                  <strong style={{
                    color: 'red',
                    padding: '5px 10px',
                    border: '1px solid red',
                    borderRadius: '20px'
                  }}>Khó</strong>
                )
              }
              {
                question?.examTagNameStr && (
                  <div style={{display: 'flex', marginLeft: '24px'}}>
                    <strong style={{fontStyle: "italic"}}># {question?.examTagNameStr}</strong>
                  </div>
                )
              }
            </div>
            <div style={{display: 'flex'}}>
              <h4 style={{marginRight: '5px', marginTop: 0}}>Môn học:</h4>
              <p style={{marginTop: 0, marginBottom: 0}}>{question?.examSubjectName}</p>
            </div>
            <div>
              <h4 style={{marginRight: '5px', marginTop: 0}}>Nội dung câu hỏi:</h4>
              <p style={{marginTop: 0, marginBottom: 0}}>{parseHTMLToString(question?.content)}</p>
              {
                (question?.filePath) && (
                  question?.filePath.split(';').map(item => {
                    return (
                      <div style={{display: 'flex', alignItems: 'center'}}>
                        <AttachFileOutlined></AttachFileOutlined>
                        <p style={{fontWeight: 'bold', cursor: 'pointer'}}
                           onClick={() => handleOpenFilePreviewDialog(item)}>{getFilenameFromString(item)}</p>
                      </div>
                    )
                  })
                )
              }
            </div>
            <div style={{display: 'flex'}}>
              <h4 style={{marginRight: '5px', marginTop: 0}}>Loại câu hỏi:</h4>
              <p style={{marginTop: 0, marginBottom: 0}}>{question?.type === 0 ? 'Trắc nghiệm' : 'Tự luận'}</p>
            </div>
            {
              (question?.type === 0) && (
                <>
                  <div style={{display: 'flex'}}>
                    <h4 style={{marginRight: '5px', marginTop: 0}}>Số đáp án:</h4>
                    <p style={{marginTop: 0, marginBottom: 0}}>{question?.numberAnswer}</p>
                  </div>
                  <div>
                    <h4 style={{marginRight: '5px', marginTop: 0}}>Nội dung phương án 1:</h4>
                    <p style={{marginTop: 0, marginBottom: 0}}>{parseHTMLToString(question?.contentAnswer1)}</p>
                    {question?.contentFileAnswer1 && (
                      <img src={getFilePathFromString(question?.contentFileAnswer1)} alt="" style={{maxHeight: "150px"}}/>
                    )}
                  </div>
                  {
                    (question?.numberAnswer >= 2) && (
                      <div>
                        <h4 style={{marginRight: '5px', marginTop: 0}}>Nội dung phương án 2:</h4>
                        <p style={{marginTop: 0, marginBottom: 0}}>{parseHTMLToString(question?.contentAnswer2)}</p>
                        {question?.contentFileAnswer2 && (
                          <img src={getFilePathFromString(question?.contentFileAnswer2)} alt="" style={{maxHeight: "150px"}}/>
                        )}
                      </div>
                    )
                  }
                  {
                    (question?.numberAnswer >= 3) && (
                      <div>
                        <h4 style={{marginRight: '5px', marginTop: 0}}>Nội dung phương án 3:</h4>
                        <p style={{marginTop: 0, marginBottom: 0}}>{parseHTMLToString(question?.contentAnswer3)}</p>
                        {question?.contentFileAnswer3 && (
                          <img src={getFilePathFromString(question?.contentFileAnswer3)} alt="" style={{maxHeight: "150px"}}/>
                        )}
                      </div>
                    )
                  }
                  {
                    (question?.numberAnswer >= 4) && (
                      <div>
                        <h4 style={{marginRight: '5px', marginTop: 0}}>Nội dung phương án 4:</h4>
                        <p style={{marginTop: 0, marginBottom: 0}}>{parseHTMLToString(question?.contentAnswer4)}</p>
                        {question?.contentFileAnswer4 && (
                          <img src={getFilePathFromString(question?.contentFileAnswer4)} alt="" style={{maxHeight: "150px"}}/>
                        )}
                      </div>
                    )
                  }
                  {
                    (question?.numberAnswer >= 5) && (
                      <div>
                        <h4 style={{marginRight: '5px', marginTop: 0}}>Nội dung phương án 5:</h4>
                        <p style={{marginTop: 0, marginBottom: 0}}>{parseHTMLToString(question?.contentAnswer5)}</p>
                        {question?.contentFileAnswer5 && (
                          <img src={getFilePathFromString(question?.contentFileAnswer5)} alt="" style={{maxHeight: "150px"}}/>
                        )}
                      </div>
                    )
                  }
                  <div style={{display: 'flex'}}>
                    <h4 style={{marginRight: '5px', marginTop: 0}}>Nhiều lựa chọn:</h4>
                    <p style={{marginTop: 0, marginBottom: 0}}>{question?.multichoice ? 'Có' : 'không'}</p>
                  </div>
                </>
              )
            }
            <div>
              <h4 style={{marginRight: '5px', marginTop: 0}}>Nội dung đáp án:</h4>
              <p style={{marginTop: 0, marginBottom: 0}}>{parseHTMLToString(question?.answer)}</p>
            </div>
            <div>
              <h4 style={{marginRight: '5px', marginTop: 0}}>Nội dung giải thích:</h4>
              <p>{parseHTMLToString(question?.explain)}</p>
            </div>
            <QuestionFilePreview
              open={openFilePreviewDialog}
              setOpen={setOpenFilePreviewDialog}
              file={filePreview}>
            </QuestionFilePreview>
          </div>
        }
        actions={
          <TertiaryButton
            variant="outlined"
            onClick={closeDialog}
          >
            Hủy
          </TertiaryButton>
        }
      />
    </div>
  );
}

const screenName = "MENU_EXAM_QUESTION_BANK";
// export default withScreenSecurity(QuestionBank, screenName, true);
export default QuestionBankDetails;
