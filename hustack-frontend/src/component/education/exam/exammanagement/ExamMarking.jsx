import React, {useEffect, useState} from 'react';
import {
  Box,
  CircularProgress,
  FormControlLabel,
  Checkbox,
  FormGroup,
  TextField,
  Radio,
  RadioGroup
} from "@mui/material";
import {formatDateTime} from "../ultils/DateUltils";
import {request} from "../../../../api";
import {toast} from "react-toastify";
import {Scoreboard} from "@mui/icons-material";
import {AccessTime, AttachFileOutlined, Cancel, Comment, Timer, CheckCircle, Check, Delete} from "@mui/icons-material";
import {
  getFileCommentFromFileAnswerAndExamResultDetailsId,
  getFileFromListFileAndFileAnswerAndExamResultDetailsId,
  getFileIdFromString,
  getFilenameFromFileNew,
  getFilenameFromString,
  getFilePathFromString
} from "../ultils/FileUltils";
import RichTextEditor from "../../../common/editor/RichTextEditor";
import QuestionFilePreview from "../questionbank/QuestionFilePreview";
import {parseHTMLToString} from "../ultils/DataUltils";
import CustomizedDialogs from "../../../dialog/CustomizedDialogs";
import {makeStyles} from "@material-ui/core/styles";
import PrimaryButton from "../../../button/PrimaryButton";
import TertiaryButton from "../../../button/TertiaryButton";
import TextEditor from "../ultils/component/TextEditor";

const useStyles = makeStyles((theme) => ({
  dialogContent: {minWidth: '90vw'},
}));

function ExamMarking(props) {
  const classes = useStyles();
  const { open, setOpen, data, setDataDetails} = props;

  const [isLoading, setIsLoading] = useState(false);
  const [dataAnswers, setDataAnswers] = useState([]);
  const [totalScore, setTotalScore] = useState(0);
  const [comment, setComment] = useState(data?.comment ? data?.comment : '');
  const [fileComments, setFileComments] = useState([]);
  const [openFilePreviewDialog, setOpenFilePreviewDialog] = useState(false);
  const [filePreview, setFilePreview] = useState(null);
  const [examResultDetailsIdSelected, setExamResultDetailsIdSelected] = useState(null);
  const [indexSelected, setIndexSelected] = useState(null);
  const [isComment, setIsComment] = useState(false);
  const [imageComment, setImageComment] = useState(null);
  const [contentComment, setContentComment] = useState(null);
  const [commentFilePathDeletes, setCommentFilePathDeletes] = useState([]);
  const [isTyping, setIsTyping] = useState(false);

  useEffect(() => {
    let tmpDataAnswers = []
    let totalScore = 0
    for(let item of data?.questionList){
      let score = 0;
      if(item?.questionType === 0){
        if(checkAnswerRadioAndCheckbox(item?.questionType, item?.questionAnswer, item?.answer)){
          score = 1
        }
      }
      if(item?.score){
        score = item?.score
      }
      tmpDataAnswers.push({
        questionOrder: item?.questionOrder,
        id: item?.examResultDetailsId,
        examResultId: data?.examResultId,
        examQuestionId: item?.questionId,
        answer: item?.answer,
        filePath: item?.filePathAnswer,
        commentFilePath: item?.filePathComment,
        score: score
      })
      totalScore += score
    }

    setDataAnswers(tmpDataAnswers)
    setTotalScore(totalScore)
  }, []);

  useEffect(() => {
    if(imageComment){
      setFileComments(fileComments.concat(imageComment))
      setImageComment(null)
    }
  }, [imageComment]);

  useEffect(() => {
    if(contentComment){
      setComment(comment.concat(contentComment))
      setIsTyping(false)
      setContentComment(null)
    }
  }, [contentComment]);

  const handleMarking = () => {
    const body = {
      examResultId: data?.examResultId,
      totalScore: totalScore,
      comment: comment,
      commentFilePathDeletes: commentFilePathDeletes,
      examResultDetails: dataAnswers
    }

    let formData = new FormData();
    formData.append("body", new Blob([JSON.stringify(body)], {type: 'application/json'}));
    for (const file of fileComments) {
      formData.append("files", file);
    }

    const config = {
      headers: {
        "content-type": "multipart/form-data",
      },
    };

    setIsLoading(true)
    request(
      "post",
      '/exam/teacher/submissions',
      (res) => {
        if(res.status === 200){
          if(res.data.resultCode === 200){
            toast.success(res.data.resultMsg)
            setIsLoading(false)
            handleExamDetails()
          }else{
            toast.error(res.data.resultMsg)
            setIsLoading(false)
          }
        }else {
          toast.error(res)
          setIsLoading(false)
        }
      },
      { onError: (e) => toast.error(e) },
      formData,
      config,
    );
  }

  const handleExamDetails = () => {
    request(
      "get",
      `/exam/${data?.examId}`,
      (res) => {
        if(res.data.resultCode === 200){
          setDataDetails(res.data.data)
          closeDialog()
        }else{
          toast.error(res.data.resultMsg)
        }
      },
      { onError: (e) => toast.error(e) },
    );
  };

  const handleOpenFilePreviewDialog = (data, index, isComment, examResultDetailsId) => {
    setOpenFilePreviewDialog(true)
    setFilePreview(getFilePathFromString(data))
    if(!dataAnswers[index]?.commentFilePath?.includes(`${examResultDetailsId}_${getFileIdFromString(data)}`) &&
      !getFilenameFromFileNew(fileComments)?.includes(`${examResultDetailsId}_${getFileIdFromString(data)}`)){
      setExamResultDetailsIdSelected(examResultDetailsId)
      setIndexSelected(index)
      setIsComment(isComment)
    }else{
      setIsComment(false)
    }
  };

  const handleMarkingScore = (event, questionOrder) => {
    let tmpScore = 0;
    if(event.target.value === ''){
      dataAnswers[questionOrder-1].score = 0
    }else{
      dataAnswers[questionOrder-1].score = parseInt(event.target.value)
    }

    let totalScore = 0
    for(let item of dataAnswers){
      totalScore += item?.score
    }

    setDataAnswers(dataAnswers)
    setTotalScore(totalScore)
  };

  const checkAnswerRadioAndCheckbox = (questionType, answerQuestion, answerStudent) => {
    if(questionType === 0){
      const answerQuestions = answerQuestion.split(',').sort();
      const answerStudents = answerStudent.split(',').sort();

      return answerStudents.every(elem => answerQuestions.includes(elem));
    }
  }

  const closeDialog = () => {
    setOpen(false)
  }

  const handleKeyPress = (event) => {
    const regex = /^[0-9]+$/
    if (!regex.test(event.key)) {
      event.preventDefault();
    }
  }

  const deleteCommentFileNew = (fileAnswer, examResultDetailsId) => {
    const file = getFileFromListFileAndFileAnswerAndExamResultDetailsId(fileComments, fileAnswer, examResultDetailsId);
    if(file){
      setFileComments(fileComments.filter(f => f !== file));
    }
  }

  const deleteCommentFileExist = (filePath, index) => {
    setDataAnswers(prevDataAnswers => {
      return prevDataAnswers.map((item, i) => {
        if (i === index && item?.commentFilePath) {
          return {
            ...item,
            commentFilePath: item.commentFilePath
              .split(";")
              .filter(path => path !== filePath)
              .join(";"),
          };
        }
        return item;
      });
    });
    setCommentFilePathDeletes(commentFilePathDeletes.concat(filePath))
  }

  return (
    <div>
      <CustomizedDialogs
        open={open}
        classNames={{paper: classes.dialogContent}}
        handleClose={closeDialog}
        title={`Bài thi của ${data?.examStudentName}(${data?.examStudentCode})`}
        content={
          <div>
            <div style={{display: "flex", justifyContent: "space-between", width: '100%'}}>
              <div>
                <div style={{display: "flex", alignItems: "center", marginBottom: '10px'}}>
                  <Scoreboard/>
                  <p style={{padding: 0, margin: 0}}><strong>Tổng điểm: </strong> {totalScore}</p>
                </div>
                <div style={{display: "flex", alignItems: "center", marginBottom: '10px'}}>
                  <Timer/>
                  <p style={{padding: 0, margin: 0}}><strong>Tổng thời gian làm: </strong> {data?.totalTime} (phút)</p>
                </div>
                <div style={{display: "flex", alignItems: "center", marginBottom: '10px'}}>
                  <AccessTime/>
                  <p style={{padding: 0, margin: 0}}><strong>Thời gian nộp: </strong> {formatDateTime(data?.submitedAt)}
                  </p>
                </div>
              </div>
              <div style={{display: "flex", flexDirection: "column"}}>
                <p style={{margin: 0, padding: 0, display: "flex"}}>
              <span style={{
                fontWeight: "bold",
                marginRight: '5px'
              }}>Email:</span>{data?.examStudentEmail}</p>
                <p style={{margin: 0, padding: 0, display: "flex"}}>
                  <span style={{fontWeight: "bold", marginRight: '5px'}}>Phone:</span>{data?.examStudentPhone}
                </p>
              </div>
            </div>

            {
              data?.questionList?.map((value, index) => {
                const questionOrder = value?.questionOrder;
                return (
                  <div
                    key={questionOrder}
                    style={{
                      border: '2px solid #f5f5f5',
                      borderColor:
                        value?.questionType === 0 ?
                          (checkAnswerRadioAndCheckbox(value?.questionType, value?.questionAnswer, value?.answer) ? '#61bd6d' : '#f50000c9') :
                          '#f5f5f5',
                      display: 'flex',
                      justifyContent: 'space-between',
                      borderRadius: '10px',
                      padding: '10px',
                      marginBottom: '10px'
                    }}>
                    <Box display="flex"
                         flexDirection='column'
                         width="100%">
                      <div style={{display: "flex", justifyContent: "space-between"}}>
                        <div style={{display: 'flex'}}>
                        <span style={{
                          display: "block",
                          fontWeight: 'bold',
                          marginRight: '5px'
                        }}>Câu {value?.questionOrder}.</span>
                          <span
                            style={{fontStyle: 'italic'}}>({value?.questionType === 0 ? 'Trắc nghiệm' : 'Tự luận'})</span>
                        </div>

                        <div style={{display: "flex", alignItems: "center"}} key={questionOrder}>
                          {
                            value?.questionType === 0 ?
                              (checkAnswerRadioAndCheckbox(value?.questionType, value?.questionAnswer, value?.answer) ?
                                <CheckCircle style={{color: '#61bd6d'}}/> :
                                <Cancel style={{color: '#f50000c9'}}/>) :
                              (<></>)
                          }
                          <TextField
                            id={`scoreInput-${questionOrder}`}
                            label="Nhập điểm"
                            onKeyPress={handleKeyPress}
                            style={{width: "90px", marginLeft: "16px"}}
                            size="small"
                            value={dataAnswers[questionOrder - 1]?.score}
                            onChange={(event) => {
                              handleMarkingScore(event, questionOrder);
                            }}
                            InputLabelProps={{
                              shrink: true,
                            }}
                          />
                        </div>
                      </div>

                      <p >
                        <strong style={{marginRight: '10px'}}>Câu hỏi: </strong>{parseHTMLToString(value?.questionContent)}
                      </p>
                      {
                        value?.questionFile && (
                          value?.questionFile.split(';').map(item => {
                            return (
                              <div style={{display: 'flex', alignItems: 'center'}}>
                                <AttachFileOutlined></AttachFileOutlined>
                                <p style={{fontWeight: 'bold', cursor: 'pointer'}}
                                   onClick={() => handleOpenFilePreviewDialog(item, index,false, null)}>{getFilenameFromString(item)}</p>
                              </div>
                            )
                          })
                        )
                      }
                      {
                        value?.questionType === 0 && value?.questionMultichoice && (
                          <Box sx={{display: 'flex', flexDirection: 'column'}}>
                            <p style={{margin: 0, padding: 0, fontWeight: "bold"}}>Chọn các đáp án đúng trong các đáp án
                              sau:</p>
                            <FormControlLabel
                              label={
                                <FormGroup row>
                                  <Box display="flex" alignItems="center">
                                    <div>
                                      <p>{parseHTMLToString(value?.questionContentAnswer1)}</p>
                                      {value?.questionContentFileAnswer1 && (
                                        <img src={getFilePathFromString(value?.questionContentFileAnswer1)} alt=""
                                             style={{maxHeight: "150px"}}/>
                                      )}
                                    </div>
                                    {value?.questionAnswer?.includes('1') && (
                                      <Check style={{marginLeft: 8, color: 'green'}}/>)}
                                  </Box>
                                </FormGroup>
                              }
                              control={
                                <Checkbox color="primary"
                                          checked={value?.answer?.includes('1')}
                                          disabled/>
                              }
                            />
                            {
                              value?.questionNumberAnswer >= 2 && (
                                <FormControlLabel
                                  label={
                                    <FormGroup row>
                                      <Box display="flex" alignItems="center">
                                        <div>
                                          <p>{parseHTMLToString(value?.questionContentAnswer2)}</p>
                                          {value?.questionContentFileAnswer2 && (
                                            <img src={getFilePathFromString(value?.questionContentFileAnswer2)} alt=""
                                                 style={{maxHeight: "150px"}}/>
                                          )}
                                        </div>
                                        {value?.questionAnswer?.includes('2') && (
                                          <Check style={{marginLeft: 8, color: 'green'}}/>)}
                                      </Box>
                                    </FormGroup>
                                  }
                                  control={
                                    <Checkbox color="primary"
                                              checked={value?.answer?.includes('2')}
                                              disabled/>
                                  }
                                />
                              )
                            }
                            {
                              value?.questionNumberAnswer >= 3 && (
                                <FormControlLabel
                                  label={
                                    <FormGroup row>
                                      <Box display="flex" alignItems="center">
                                        <div>
                                          <p>{parseHTMLToString(value?.questionContentAnswer3)}</p>
                                          {value?.questionContentFileAnswer3 && (
                                            <img src={getFilePathFromString(value?.questionContentFileAnswer3)} alt=""
                                                 style={{maxHeight: "150px"}}/>
                                          )}
                                        </div>
                                        {value?.questionAnswer?.includes('3') && (
                                          <Check style={{marginLeft: 8, color: 'green'}}/>)}
                                      </Box>
                                    </FormGroup>
                                  }
                                  control={
                                    <Checkbox color="primary"
                                              checked={value?.answer?.includes('3')}
                                              disabled/>
                                  }
                                />
                              )
                            }
                            {
                              value?.questionNumberAnswer >= 4 && (
                                <FormControlLabel
                                  label={
                                    <FormGroup row>
                                      <Box display="flex" alignItems="center">
                                        <div>
                                          <p>{parseHTMLToString(value?.questionContentAnswer4)}</p>
                                          {value?.questionContentFileAnswer4 && (
                                            <img src={getFilePathFromString(value?.questionContentFileAnswer4)} alt=""
                                                 style={{maxHeight: "150px"}}/>
                                          )}
                                        </div>
                                        {value?.questionAnswer?.includes('4') && (
                                          <Check style={{marginLeft: 8, color: 'green'}}/>)}
                                      </Box>
                                    </FormGroup>
                                  }
                                  control={
                                    <Checkbox color="primary"
                                              checked={value?.answer?.includes('4')}
                                              disabled/>
                                  }
                                />
                              )
                            }
                            {
                              value?.questionNumberAnswer >= 5 && (
                                <FormControlLabel
                                  label={
                                    <FormGroup row>
                                      <Box display="flex" alignItems="center">
                                        <div>
                                          <p>{parseHTMLToString(value?.questionContentAnswer5)}</p>
                                          {value?.questionContentFileAnswer5 && (
                                            <img src={getFilePathFromString(value?.questionContentFileAnswer5)} alt=""
                                                 style={{maxHeight: "150px"}}/>
                                          )}
                                        </div>
                                        {value?.questionAnswer?.includes('5') && (
                                          <Check style={{marginLeft: 8, color: 'green'}}/>)}
                                      </Box>
                                    </FormGroup>
                                  }
                                  control={
                                    <Checkbox color="primary"
                                              checked={value?.answer?.includes('5')}
                                              disabled/>
                                  }
                                />
                              )
                            }
                          </Box>
                        )
                      }
                      {
                        value?.questionType === 0 && !value?.questionMultichoice && (
                          <Box sx={{display: 'flex', flexDirection: 'column'}}>
                            <p style={{margin: 0, padding: 0, fontWeight: "bold"}}>Chọn đáp án đúng nhất:</p>
                            <RadioGroup
                              aria-labelledby="demo-radio-buttons-group-label"
                              name="radio-buttons-group"
                            >
                              <FormControlLabel
                                value="1"
                                control={
                                  <Radio
                                    checked={value?.answer?.includes('1')}
                                    disabled
                                  />
                                }
                                label={
                                  <FormGroup row>
                                    <Box display="flex" alignItems="center">
                                      <div>
                                        <p>{parseHTMLToString(value?.questionContentAnswer1)}</p>
                                        {value?.questionContentFileAnswer1 && (
                                          <img src={getFilePathFromString(value?.questionContentFileAnswer1)} alt=""
                                               style={{maxHeight: "150px"}}/>
                                        )}
                                      </div>
                                      {value?.questionAnswer?.includes('1') && (
                                        <Check style={{marginLeft: 8, color: 'green'}}/>)}
                                    </Box>
                                  </FormGroup>
                                }
                              />
                              {
                                value?.questionNumberAnswer >= 2 && (
                                  <FormControlLabel
                                    value="2"
                                    control={
                                      <Radio
                                        checked={value?.answer?.includes('2')}
                                        disabled
                                      />
                                    }
                                    label={
                                      <FormGroup row>
                                        <Box display="flex" alignItems="center">
                                          <div>
                                            <p>{parseHTMLToString(value?.questionContentAnswer2)}</p>
                                            {value?.questionContentFileAnswer2 && (
                                              <img src={getFilePathFromString(value?.questionContentFileAnswer2)} alt=""
                                                   style={{maxHeight: "150px"}}/>
                                            )}
                                          </div>
                                          {value?.questionAnswer?.includes('2') && (
                                            <Check style={{marginLeft: 8, color: 'green'}}/>)}
                                        </Box>
                                      </FormGroup>
                                    }
                                  />
                                )
                              }
                              {
                                value?.questionNumberAnswer >= 3 && (
                                  <FormControlLabel
                                    value="3"
                                    control={
                                      <Radio
                                        checked={value?.answer?.includes('3')}
                                        disabled
                                      />
                                    }
                                    label={
                                      <FormGroup row>
                                        <Box display="flex" alignItems="center">
                                          <div>
                                            <p>{parseHTMLToString(value?.questionContentAnswer3)}</p>
                                            {value?.questionContentFileAnswer3 && (
                                              <img src={getFilePathFromString(value?.questionContentFileAnswer3)} alt=""
                                                   style={{maxHeight: "150px"}}/>
                                            )}
                                          </div>
                                          {value?.questionAnswer?.includes('3') && (
                                            <Check style={{marginLeft: 8, color: 'green'}}/>)}
                                        </Box>
                                      </FormGroup>
                                    }
                                  />
                                )
                              }
                              {
                                value?.questionNumberAnswer >= 4 && (
                                  <FormControlLabel
                                    value="4"
                                    control={
                                      <Radio
                                        checked={value?.answer?.includes('4')}
                                        disabled
                                      />
                                    }
                                    label={
                                      <FormGroup row>
                                        <Box display="flex" alignItems="center">
                                          <div>
                                            <p>{parseHTMLToString(value?.questionContentAnswer4)}</p>
                                            {value?.questionContentFileAnswer4 && (
                                              <img src={getFilePathFromString(value?.questionContentFileAnswer4)} alt=""
                                                   style={{maxHeight: "150px"}}/>
                                            )}
                                          </div>
                                          {value?.questionAnswer?.includes('4') && (
                                            <Check style={{marginLeft: 8, color: 'green'}}/>)}
                                        </Box>
                                      </FormGroup>
                                    }
                                  />
                                )
                              }
                              {
                                value?.questionNumberAnswer >= 5 && (
                                  <FormControlLabel
                                    value="5"
                                    control={
                                      <Radio
                                        checked={value?.answer?.includes('5')}
                                        disabled
                                      />
                                    }
                                    label={
                                      <FormGroup row>
                                        <Box display="flex" alignItems="center">
                                          <div>
                                            <p>{parseHTMLToString(value?.questionContentAnswer5)}</p>
                                            {value?.questionContentFileAnswer5 && (
                                              <img src={getFilePathFromString(value?.questionContentFileAnswer5)} alt=""
                                                   style={{maxHeight: "150px"}}/>
                                            )}
                                          </div>
                                          {value?.questionAnswer?.includes('5') && (
                                            <Check style={{marginLeft: 8, color: 'green'}}/>)}
                                        </Box>
                                      </FormGroup>
                                    }
                                  />
                                )
                              }
                            </RadioGroup>
                          </Box>
                        )
                      }
                      {
                        value?.questionType === 1 && (
                          <div>
                            <strong style={{marginRight: '10px'}}>Trả lời:</strong>{parseHTMLToString(value?.answer)}
                          </div>
                        )
                      }
                      {
                        value?.questionType === 1 && value?.filePathAnswer != null && value?.filePathAnswer !== '' && (
                          <div style={{marginTop: '10px'}}>
                            <strong>File trả lời đính kèm:</strong>
                            {
                              value?.filePathAnswer.split(';').map(item => {
                                return (
                                  <div>
                                    <div style={{display: 'flex', alignItems: 'center'}}>
                                      <AttachFileOutlined></AttachFileOutlined>
                                      <p style={{fontWeight: 'bold', cursor: 'pointer'}}
                                         onClick={() => handleOpenFilePreviewDialog(item, index, true, value?.examResultDetailsId)}>{getFilenameFromString(item)}</p>
                                    </div>
                                    {
                                      fileComments.length > 0 && getFileFromListFileAndFileAnswerAndExamResultDetailsId(fileComments, item, value?.examResultDetailsId) && (
                                        <div style={{display: 'flex', alignItems: 'center', marginLeft: "22px"}}>
                                          <Comment style={{color: '#1e88e5'}}/>
                                          <p style={{
                                            color: '#1e88e5',
                                            fontWeight: 'bold',
                                            margin: "0 3px"
                                          }}>(New) Nhận xét về {getFilenameFromString(item)}</p>
                                          <Delete
                                            style={{cursor: 'pointer', color: 'red', marginLeft: "12px"}}
                                            onClick={() => deleteCommentFileNew(item, value?.examResultDetailsId)}
                                          />
                                        </div>
                                      )
                                    }
                                    {
                                      dataAnswers[index]?.commentFilePath && getFileCommentFromFileAnswerAndExamResultDetailsId(dataAnswers[index]?.commentFilePath, item, value?.examResultDetailsId) && (
                                        <div style={{display: 'flex', alignItems: 'center', marginLeft: "22px"}}>
                                          <Comment style={{color: 'green'}}/>
                                          <p style={{color: 'green', fontWeight: 'bold', cursor: 'pointer', margin: "0 3px"}}
                                             onClick={() => handleOpenFilePreviewDialog(getFileCommentFromFileAnswerAndExamResultDetailsId(dataAnswers[index]?.commentFilePath, item, value?.examResultDetailsId), index, false)}
                                          >Nhận xét về {getFilenameFromString(item)}</p>
                                          <Delete
                                            style={{cursor: 'pointer', color: 'red', marginLeft: "12px"}}
                                            onClick={() => deleteCommentFileExist(getFileCommentFromFileAnswerAndExamResultDetailsId(dataAnswers[index]?.commentFilePath, item, value?.examResultDetailsId), index)}
                                          />
                                        </div>
                                      )
                                    }
                                  </div>
                                )
                              })
                            }
                          </div>
                        )
                      }
                      {
                        value?.questionType === 1 && (
                          <div>
                            <strong style={{marginRight: '10px'}}>Đáp
                              án:</strong>{parseHTMLToString(value?.questionAnswer)}
                          </div>
                        )
                      }
                      <div>
                        <strong style={{marginRight: '10px'}}>Giải
                          thích:</strong>{parseHTMLToString(value?.questionExplain)}
                      </div>
                    </Box>
                  </div>
                )
              })
            }

            {/*<div>*/}
            {/*  <h4 style={{marginBottom: 0, fontSize: '18px'}}>File đính kèm:</h4>*/}
            {/*  {*/}
            {/*    (data?.answerFiles == null || data?.answerFiles == '') ?*/}
            {/*      (*/}
            {/*        <div>N/A</div>*/}
            {/*      ) :*/}
            {/*      (*/}
            {/*        data?.answerFiles.split(';').map(item => {*/}
            {/*          return (*/}
            {/*            <div style={{display: 'flex', alignItems: 'center'}}>*/}
            {/*              <AttachFileOutlined></AttachFileOutlined>*/}
            {/*              <p style={{fontWeight: 'bold', cursor: 'pointer'}}*/}
            {/*                 onClick={() => handleOpenFilePreviewDialog(item)}>{getFilenameFromString(item)}</p>*/}
            {/*            </div>*/}
            {/*          )*/}
            {/*        })*/}
            {/*      )*/}
            {/*  }*/}
            {/*</div>*/}

            <div>
              <h4 style={{marginBottom: 0, fontSize: '18px'}}>Nhận xét:</h4>
              <TextEditor
                content={comment}
                onContentChange={(value) => {
                  setComment(value)
                  setIsTyping(true)
                }
                }
                isTyping={isTyping}
              />
            </div>
          </div>
        }
        actions={
          <div>
            <TertiaryButton
              variant="outlined"
              onClick={closeDialog}
            >
              Hủy
            </TertiaryButton>
            <PrimaryButton
              disabled={isLoading}
              variant="contained"
              color="primary"
              style={{marginLeft: "15px"}}
              onClick={handleMarking}
              type="submit"
            >
              {isLoading ? <CircularProgress/> : "Chấm bài"}
            </PrimaryButton>
          </div>
        }
      />
      <QuestionFilePreview
        open={openFilePreviewDialog}
        setOpen={setOpenFilePreviewDialog}
        file={filePreview}
        examResultDetailsIdSelected={examResultDetailsIdSelected}
        indexSelected={indexSelected}
        isComment={isComment}
        imageComment={imageComment}
        setImageComment={setImageComment}
        setContentComment={setContentComment}
      >
      </QuestionFilePreview>
    </div>
  );
}

const screenName = "MENU_EXAM_MANAGEMENT";
// export default withScreenSecurity(QuestionBank, screenName, true);
export default ExamMarking;
