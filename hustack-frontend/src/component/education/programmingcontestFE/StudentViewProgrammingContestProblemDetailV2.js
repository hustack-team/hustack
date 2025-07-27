import {LoadingButton} from "@mui/lab";
import {Alert, Box, Collapse, Divider, Stack, Typography} from "@mui/material";
import {grey} from "@mui/material/colors";
import {styled} from "@mui/material/styles";
import HustCopyCodeBlock from "component/common/HustCopyCodeBlock";
import HustModal from "component/common/HustModal";
import {ContentState, EditorState} from "draft-js";
import htmlToDraft from "html-to-draftjs";
import React, {forwardRef, useEffect, useRef, useState} from "react";
import {Editor} from "react-draft-wysiwyg";
import {useParams} from "react-router";
import {errorNoti, successNoti} from "utils/notification";
import {request, saveFile} from "../../../api";
import FileUploadZone from "../../../utils/FileUpload/FileUploadZone";
import HustCodeEditor from "../../common/HustCodeEditor";
import HustCodeLanguagePicker from "../../common/HustCodeLanguagePicker";
import {
  COMPUTER_LANGUAGES,
  DEFAULT_CODE_SEGMENT_C,
  DEFAULT_CODE_SEGMENT_CPP,
  DEFAULT_CODE_SEGMENT_JAVA,
  DEFAULT_CODE_SEGMENT_PYTHON,
  mapLanguageToCodeBlockLanguage,
  mapLanguageToDisplayName,
  SUBMISSION_MODE_NOT_ALLOWED,
  SUBMISSION_MODE_SOURCE_CODE,
} from "./Constant";
import StudentViewSubmission from "./StudentViewSubmission";
import {useTranslation} from "react-i18next";
import _ from "lodash";
import ProgrammingContestLayout from "./ProgrammingContestLayout";
import {useHistory} from "react-router-dom";
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown';
import TertiaryButton from "component/button/TertiaryButton";
import {AntTab, AntTabs} from "component/tab";
import RotatingIconButton from "../../common/RotatingIconButton";

const VisuallyHiddenInput = styled("input")({
  clip: "rect(0 0 0 0)",
  clipPath: "inset(50%)",
  height: 1,
  overflow: "hidden",
  position: "absolute",
  bottom: 0,
  left: 0,
  whiteSpace: "nowrap",
  width: 1,
});

export const InputFileUpload = forwardRef((props, ref) => {
  const {label, buttonProps, ...otherProps} = props;
  return (
    <TertiaryButton
      component="label"
      sx={{textTransform: "none"}}
      {...buttonProps}
    >
      {label}
      <VisuallyHiddenInput type="file" ref={ref} {...otherProps} />
    </TertiaryButton>
  );
});

const editorStyle = {
  editor: {
    // border: "1px solid black",
    // minHeight: "300px",
  },
};

const ERR_STATUS = [
  "NOT_FOUND",
  "NOT_ALLOWED_TO_SUBMIT",
  "NOT_ALLOWED_LANGUAGE",
  "PARTICIPANT_NOT_REGISTERED_OR_APPROVED",
  "PARTICIPANT_HAS_NO_PERMISSION_TO_SUBMIT",
  "SUBMISSION_NOT_ALLOWED",
  "MAX_NUMBER_SUBMISSIONS_REACHED",
  "SOURCE_CODE_REQUIRED",
  "MAX_SOURCE_CODE_LENGTH_VIOLATIONS",
  "SUBMISSION_INTERVAL_VIOLATIONS",
];

export default function StudentViewProgrammingContestProblemDetail() {
  const {problemId, contestId} = useParams();
  const history = useHistory();
  const {t} = useTranslation(["education/programmingcontest/problem", "common"]);

  const [problem, setProblem] = useState(null);
  const [testCases, setTestCases] = useState([]);
  const [file, setFile] = useState(null);
  const [language, setLanguage] = useState(COMPUTER_LANGUAGES.CPP17);
  const [listLanguagesAllowed, setListLanguagesAllowed] = useState([]);
  const [codeSolution, setCodeSolution] = useState("");
  const [submissionMode, setSubmissionMode] = useState(
    SUBMISSION_MODE_SOURCE_CODE
  );
  const [isSubmitCode, setIsSubmitCode] = useState(0);
  const [openModalPreview, setOpenModalPreview] = useState(false);
  const [selectedTestcase, setSelectedTestcase] = useState();
  const [isProcessing, setIsProcessing] = React.useState(false);
  const [editorStateDescription, setEditorStateDescription] = useState(
    EditorState.createEmpty()
  );
  const [sampleTestCase, setSampleTestCase] = useState(
    null//EditorState.createEmpty()
  );
  const [fetchedImageArray, setFetchedImageArray] = useState([]);
  const [isProblemBlock, setIsProblemBlock] = useState(false);
  const [blockCodes, setBlockCodes] = useState([]);
  const [selectedLanguage, setSelectedLanguage] = useState(null);
  const [blockCodeInputs, setBlockCodeInputs] = useState({});
  const [isBlockCodesExpanded, setIsBlockCodesExpanded] = useState(true);
  const [rotationCount, setRotationCount] = useState(0);

  const inputRef = useRef();
  const listSubmissionRef = useRef(null);

  function onFileChange(event) {
    setFile(event.target.files[0]);
  }

  const onInputChange = (event) => {
    let name = event.target.value;
    setFile(name);
  };

  async function isFileBlank(file) {
    if (!file) return true;

    const content = await readFileAsText(file);
    return _.isEmpty(_.trim(content));
  }

  function readFileAsText(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => resolve(e.target.result);
      reader.onerror = (e) => {
        errorNoti(t("education/programmingcontest/testcase:errorReadingFile"));
        reject(e);
      };
      reader.readAsText(file);
    });
  }

  const handleCopyAllBlocks = async () => {
    const blocksForLanguage = blockCodes
      .filter(block => block.language === selectedLanguage)
      .sort((a, b) => a.seq - b.seq);

    if (blocksForLanguage.length === 0) {
      errorNoti(t("common:noBlockCodesToCopy"), 3000);
      return;
    }

    const combinedCode = blocksForLanguage
      .map(block => {
        const code = block.forStudent ? (blockCodeInputs[block.id] || "") : block.code;
        return code;
      })
      .join("\n");

    await navigator.clipboard.writeText(combinedCode).then(() => {
      successNoti(t('common:copySuccess'), 2000);
    });
  };

  const handleFormSubmit = async (event) => {
    if (event) event.preventDefault();
    setIsProcessing(true);

    const body = {
      problemId: problemId,
      contestId: contestId,
      language: isProblemBlock ? selectedLanguage : language,
      isProblemBlock: isProblemBlock ? 1 : 0
    };

    if (isProblemBlock) {
      body.blockCodes = blockCodes
        .filter(block => block.language === selectedLanguage && block.forStudent)
        .sort((a, b) => a.seq - b.seq)
        .map(block => ({
          seq: block.seq,
          code: blockCodeInputs[block.id] || "",
          language: block.language
        }));
    }

    // Validate submission data
    if (isProblemBlock) {
      if (isBlockCodeBlank(body.blockCodes)) {
        errorNoti(t("common:sourceCodeRequired"), 3000);
        setIsProcessing(false);
        return;
      }
    } else {
      if (await isFileBlank(file)) {
        errorNoti(t("common:sourceCodeRequired"), 3000);
        setIsProcessing(false);
        return;
      }
    }

    const formData = new FormData();
    formData.append("dto", new Blob([JSON.stringify(body)], {type: 'application/json'}));
    if (!isProblemBlock) {
      formData.append("file", file);
    }

    const config = {
      headers: {
        "content-Type": "multipart/form-data",
      },
    };

    //TODO: consider remove duplicate code
    request(
      "post",
      "/submissions/file-upload",
      (res) => {
        setIsProcessing(false);
        res = res.data;
        listSubmissionRef.current.refreshSubmission();

        if (inputRef.current) {
          inputRef.current.value = null;
        }
        setFile(null);

        if (isProblemBlock) {
          const resetInputs = {};
          blockCodes.forEach(block => {
            if (block.forStudent) {
              resetInputs[block.id] = block.code || "";
            }
          });
          setBlockCodeInputs(resetInputs);
        } else {
          switch (language) {
            case COMPUTER_LANGUAGES.C:
              setCodeSolution(DEFAULT_CODE_SEGMENT_C);
              break;
            case COMPUTER_LANGUAGES.CPP11:
            case COMPUTER_LANGUAGES.CPP14:
            case COMPUTER_LANGUAGES.CPP17:
              setCodeSolution(DEFAULT_CODE_SEGMENT_CPP);
              break;
            case COMPUTER_LANGUAGES.JAVA:
              setCodeSolution(DEFAULT_CODE_SEGMENT_JAVA);
              break;
            case COMPUTER_LANGUAGES.PYTHON:
              setCodeSolution(DEFAULT_CODE_SEGMENT_PYTHON);
              break;
            default:
              setCodeSolution("");
          }
        }

        if (ERR_STATUS.includes(res.status)) {
          errorNoti(res.message, 3000);
        } else {
          successNoti(t("common:submitted"), 3000);
        }
      },
      {
        onError: (e) => {
          setIsProcessing(false);
          setFile(null);
          if (inputRef.current) {
            inputRef.current.value = null;
          }
          errorNoti(t("common:error", 3000))
        },
      },
      formData,
      config
    );
  };

  function isBlockCodeBlank(blockCodes) {
    return blockCodes.every(block => _.isEmpty(_.trim(block.code)));
  }

  function getProblemDetail() {
    request(
      "get",
      "/contests/" + contestId + "/problems/" + problemId,
      (res) => {
        res = res.data;
        setProblem(res);
        if (res.listLanguagesAllowed != null && res.listLanguagesAllowed.length > 0) {
          setLanguage(res.listLanguagesAllowed[0])
          setListLanguagesAllowed(res.listLanguagesAllowed);
        }

        if (res.blockCodes && res.blockCodes.length > 0) {
          setIsProblemBlock(true);
          setBlockCodes(res.blockCodes);
          const uniqueLanguages = [...new Set(res.blockCodes.map(block => block.language))];
          setSelectedLanguage(uniqueLanguages[0]);
          const initialInputs = {};
          res.blockCodes.forEach(block => {
            if (block.forStudent) {
              initialInputs[block.id] = block.code || "";
            }
          });
          setBlockCodeInputs(initialInputs);
        } else if (res.isPreloadCode) {
          setCodeSolution(res.preloadCode);
        }

        if (res.submissionMode) setSubmissionMode(res.submissionMode);
        if (res.attachments && res.attachments.length > 0) {
          setFetchedImageArray(res.attachments);
        }

        let problemDescriptionHtml = htmlToDraft(res.problemStatement);
        let {contentBlocks, entityMap} = problemDescriptionHtml;
        let contentDescriptionState = ContentState.createFromBlockArray(
          contentBlocks,
          entityMap
        );
        let statementDescription = EditorState.createWithContent(
          contentDescriptionState
        );
        setEditorStateDescription(statementDescription);

        setSampleTestCase(res.sampleTestCase);
      },
      {onError: (e) => console.log(e)}
    );
  }

  useEffect(() => {
    getProblemDetail();
  }, []);

  useEffect(() => {
    if (problem && problem.isPreloadCode === true) return;
    switch (language) {
      case COMPUTER_LANGUAGES.C:
        setCodeSolution(DEFAULT_CODE_SEGMENT_C);
        break;
      case COMPUTER_LANGUAGES.CPP11:
      case COMPUTER_LANGUAGES.CPP14:
      case COMPUTER_LANGUAGES.CPP17:
        setCodeSolution(DEFAULT_CODE_SEGMENT_CPP);
        break;
      case COMPUTER_LANGUAGES.JAVA:
        setCodeSolution(DEFAULT_CODE_SEGMENT_JAVA);
        break;
      case COMPUTER_LANGUAGES.PYTHON:
        setCodeSolution(DEFAULT_CODE_SEGMENT_PYTHON);
        break;
    }
  }, [language, isProblemBlock, problem]);

  const ModalPreview = (chosenTestcase) => {
    return (
      <HustModal
        open={openModalPreview}
        onClose={() => setOpenModalPreview(false)}
        isNotShowCloseButton
        showCloseBtnTitle={false}
      >
        <HustCopyCodeBlock
          title="Input"
          text={chosenTestcase?.chosenTestcase?.testCase}
        />
        <HustCopyCodeBlock
          title="Output"
          text={chosenTestcase?.chosenTestcase?.correctAns}
          mt={2}
        />
      </HustModal>
    );
  };

  async function submitCode() {
    if (isProblemBlock) {
      setIsSubmitCode(isSubmitCode + 1);
    } else {
      const blob = new Blob([codeSolution], {type: "text/plain;charset=utf-8"});
      const now = new Date();
      const file = new File(
        [blob],
        `${problemId}_${now.getTime()}.txt`,
        {type: "text/plain;charset=utf-8"}
      );
      setFile(file);
      setIsSubmitCode(isSubmitCode + 1);
    }
  }

  useEffect(() => {
    if (isSubmitCode > 0) handleFormSubmit(null);
  }, [isSubmitCode]);

  const handleExit = () => {
    history.push(`/programming-contest/student-view-contest-detail/${contestId}`);
  }

  const groupedBlockCodes = blockCodes.reduce((acc, block) => {
    if (!acc[block.language]) {
      acc[block.language] = [];
    }
    acc[block.language].push(block);
    return acc;
  }, {});

  const uniqueLanguages = [...new Set(blockCodes.map(block => block.language))];

  const handleBlockCodeChange = (blockId, newCode) => {
    setBlockCodeInputs(prev => ({
      ...prev,
      [blockId]: newCode,
    }));
  };

  const handleDownloadFile = (file) => {
    request(
      "GET",
      `/contests/${contestId}/problems/${problemId}/attachments/${file.id}`,
      (res) => {
        const fileName = file.fileName || file.id;
        saveFile(fileName, res.data);
      },
      {
        403: () => errorNoti(t('common:noPermissionToDownload')),
        onError: () => errorNoti(t('common:error')),
      },
      null,
      {responseType: "blob"}
    );
  };

  return (
    <ProgrammingContestLayout title={problem ? problem.problemName : ""} onBack={handleExit}>
      <Typography variant="h6" sx={{mb: 1}}>
        {t("common:description")}
      </Typography>
      <Editor
        toolbarHidden
        editorState={editorStateDescription}
        handlePastedText={() => false}
        readOnly
        editorStyle={editorStyle.editor}
      />
      {/*
        <Typography variant="h5">{t("common:sampleTestcase")}</Typography>
        
        <Editor
          toolbarHidden
          editorState={sampleTestCase}
          handlePastedText={() => false}
          readOnly
          editorStyle={editorStyle.editor}
        />
      */}
      {/*ReactHtmlParser(sampleTestCase)*/}
      {/*sampleTestCase*/}

      {sampleTestCase && <>
        <div style={{height: "10px"}}></div>
        <HustCopyCodeBlock
          title={t("education/programmingcontest/problem:sampleTestCase")}
          text={sampleTestCase}
        />
      </>}

      {problem?.attachments && problem.attachments.length > 0 && (
        <Box sx={{mt: 2}}>
          <Typography variant="body1" sx={{mb: 1}}>{t('common:attachments')}</Typography>
          <Stack spacing={0.5}>
            {problem.attachments.map((file) => (
              <FileUploadZone
                key={file.id}
                file={file}
                onDownload={handleDownloadFile}
                removable={false}
              />
            ))}
          </Stack>
        </Box>
      )}

      <ModalPreview chosenTestcase={selectedTestcase}/>

      {problem && (
        <Box sx={{mt: 2}}>
          {isProblemBlock ? (
            <>
              <Alert
                variant="outlined"
                severity="info"
                sx={{
                  borderRadius: 1.5,
                  bgcolor: "#e5f6fd",
                  mb: 2,
                }}
              >
                {t("common:blockBasedProblemInfo")}
              </Alert>

              <Box sx={{mt: 2}}>
                <Box sx={{display: 'flex', alignItems: 'center'}}>
                  <Typography variant="body1" sx={{ml: 0}}>
                    {t("common:blockCode")}
                  </Typography>
                  <RotatingIconButton
                    onClick={() => {
                      setRotationCount(rotationCount + 1);
                      setIsBlockCodesExpanded(!isBlockCodesExpanded);
                    }}
                    aria-expanded={isBlockCodesExpanded}
                    aria-label={t("common:blockCode")}
                    color="primary"
                    size="small"
                    rotation={rotationCount * 180}
                    sx={{ml: 1}}
                  >
                    <ArrowDropDownIcon/>
                  </RotatingIconButton>
                </Box>
                <Collapse in={isBlockCodesExpanded}>
                  <AntTabs
                    value={selectedLanguage}
                    onChange={(event, newValue) => setSelectedLanguage(newValue)}
                    aria-label="programming languages tabs"
                    scrollButtons="auto"
                    variant="scrollable"
                    sx={{marginBottom: "12px"}}
                  >
                    {uniqueLanguages.map((lang) => (
                      <AntTab key={lang} label={mapLanguageToDisplayName(lang)} value={lang}
                              sx={{textTransform: 'none'}}/>
                    ))}
                  </AntTabs>

                  <Stack spacing={1}>
                    {groupedBlockCodes[selectedLanguage]
                      ?.sort((a, b) => a.seq - b.seq)
                      .map((block) => (
                        <Box
                          key={block.id}
                          sx={{
                            display: 'flex',
                            alignItems: 'flex-start',
                            gap: 2,
                          }}
                        >
                          <Box
                            sx={{
                              width: '48px',
                              minWidth: '48px',
                              display: 'flex',
                              justifyContent: 'center',
                              alignItems: 'flex-start',
                              pt: block.forStudent ? '0px' : '14px',
                            }}
                          >
                            <Typography
                              variant="body2"
                              sx={{
                                color: 'text.secondary',
                                fontWeight: 500,
                              }}
                            >
                              {block.seq}
                            </Typography>
                          </Box>
                          <Box sx={{flex: 1}}>
                            {block.forStudent ? (
                              <Box sx={{border: `1px solid ${grey[900]}`}}>
                                <HustCodeEditor
                                  language={selectedLanguage}
                                  sourceCode={blockCodeInputs[block.id] || ""}
                                  onChangeSourceCode={(code) => handleBlockCodeChange(block.id, code)}
                                  height="200px"
                                  readOnly={false}
                                  listLanguagesAllowed={[selectedLanguage]}
                                  hideProgrammingLanguage={1}
                                  blockEditor={1}
                                  minLines={5}
                                  theme="github"
                                />
                              </Box>
                            ) : (
                              <HustCopyCodeBlock
                                text={block.code}
                                language={mapLanguageToCodeBlockLanguage(selectedLanguage)}
                                showLineNumbers
                              />
                            )}
                          </Box>
                        </Box>
                      ))}
                  </Stack>

                  <Box sx={{display: 'flex', justifyContent: 'center', mt: 2, mb: 2}}>
                    <Stack direction="row" spacing={2}>
                      <TertiaryButton
                        variant="outlined"
                        startIcon={<ContentCopyIcon/>}
                        onClick={handleCopyAllBlocks}
                        disabled={!(blockCodes.length > 0)}
                      >
                        {t("common:copyCode")}
                      </TertiaryButton>

                      <LoadingButton
                        disabled={
                          isProcessing || submissionMode === SUBMISSION_MODE_NOT_ALLOWED
                        }
                        sx={{
                          minWidth: 140,
                          textTransform: 'none',
                          whiteSpace: 'nowrap'
                        }}
                        loading={isProcessing}
                        loadingIndicator={t("common:submitting")}
                        variant="contained"
                        color="primary"
                        onClick={submitCode}
                      >
                        {t("common:submit")}
                      </LoadingButton>
                    </Stack>
                  </Box>

                  {submissionMode === SUBMISSION_MODE_NOT_ALLOWED && (
                    <Box sx={{display: 'flex', justifyContent: 'center', mt: 1}}>
                      <Typography color="gray">
                        Currently, this contest problem is not open for submissions
                      </Typography>
                    </Box>
                  )}

                  {selectedLanguage === COMPUTER_LANGUAGES.JAVA && (
                    <Alert
                      variant="outlined"
                      severity="info"
                      sx={{
                        borderRadius: 1.5,
                        bgcolor: "#e5f6fd",
                        mt: 2,
                      }}
                    >
                      {t("common:javaClassDeclaration")}{" "}
                      <b>public class Main {"{...}"}</b>
                    </Alert>
                  )}
                </Collapse>
              </Box>
            </>
          ) : (
            <Box sx={{mb: 2}}>
              <HustCodeEditor
                title={t('common:sourceCode')}
                language={language}
                onChangeLanguage={(event) => setLanguage(event.target.value)}
                sourceCode={codeSolution}
                onChangeSourceCode={(code) => setCodeSolution(code)}
                height={"480px"}
                listLanguagesAllowed={listLanguagesAllowed}
              />
              <Box
                sx={{
                  width: "100%",
                  display: "flex",
                  justifyContent: "center",
                  alignItems: "center",
                  mt: 2,
                }}
              >
                <LoadingButton
                  disabled={
                    isProcessing || submissionMode === SUBMISSION_MODE_NOT_ALLOWED
                  }
                  sx={{
                    minWidth: 140,
                    textTransform: 'none',
                    whiteSpace: 'nowrap'
                  }}
                  loading={isProcessing}
                  loadingIndicator={t("common:submitting")}
                  variant="contained"
                  color="primary"
                  onClick={submitCode}
                >
                  {t("common:submit")}
                </LoadingButton>

                {submissionMode === SUBMISSION_MODE_NOT_ALLOWED && (
                  <Typography color="gray" ml={1}>
                    Currently, this contest problem is not open for submissions
                  </Typography>
                )}
              </Box>

              {language === COMPUTER_LANGUAGES.JAVA && (
                <Alert
                  variant="outlined"
                  severity="info"
                  sx={{
                    borderRadius: 1.5,
                    bgcolor: "#e5f6fd",
                    mt: 2,
                  }}
                >
                  {t("common:javaClassDeclaration")}{" "}
                  <b>public class Main {"{...}"}</b>
                </Alert>
              )}
            </Box>
          )}

          {!isProblemBlock && (
            <>
              <Divider>Or</Divider>

              <form onSubmit={handleFormSubmit}>
                <Stack alignItems={"center"} spacing={2} sx={{mt: 1}}>
                  <Stack
                    direction="row"
                    justifyContent={"center"}
                    alignItems="center"
                    spacing={4}
                  >
                    <HustCodeLanguagePicker
                      listLanguagesAllowed={listLanguagesAllowed}
                      language={language}
                      onChangeLanguage={(e) => setLanguage(e.target.value)}
                    />
                    <Stack direction="row" spacing={1} alignItems="center">
                      <InputFileUpload
                        id="selected-upload-file"
                        label={t("common:selectFile")}
                        accept=".c, .cpp, .java, .py"
                        onChange={onFileChange}
                        ref={inputRef}
                      />
                      {file && <Typography variant="body1">{file.name}</Typography>}
                    </Stack>
                  </Stack>

                  <LoadingButton
                    disabled={
                      isProcessing || submissionMode === SUBMISSION_MODE_NOT_ALLOWED
                    }
                    sx={{
                      minWidth: 140,
                      textTransform: 'none',
                      whiteSpace: 'nowrap'
                    }}
                    loading={isProcessing}
                    loadingIndicator={t("common:submitting")}
                    variant="contained"
                    color="primary"
                    type="submit"
                  >
                    {t("common:submit")}
                  </LoadingButton>
                </Stack>
              </form>
            </>
          )}


        </Box>
      )}
      {problem && language === COMPUTER_LANGUAGES.JAVA && (
        <Alert
          variant="outlined"
          severity="info"
          sx={{
            borderRadius: 1.5,
            bgcolor: "#e5f6fd",
            mt: 3,
          }}
        >
          {t("common:javaClassDeclaration")}{" "}
          <b>public class Main {"{...}"}</b>
        </Alert>
      )}

      {problem && (
        <>
          <Divider sx={{mt: 2, mb: 2}}/>
          <StudentViewSubmission problemId={problemId} ref={listSubmissionRef} showTitle={true}/>
        </>
      )}
    </ProgrammingContestLayout>
  );
}