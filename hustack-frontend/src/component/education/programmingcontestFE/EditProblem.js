import { makeStyles } from "@material-ui/core/styles";
import { LoadingButton } from "@mui/lab";
import {
  Box,
  Checkbox,
  FormControlLabel,
  Grid,
  InputAdornment,
  Stack,
  TextField,
  Typography,
  Tabs,
  Tab,
  Button,
  IconButton,
  Collapse,
} from "@mui/material";
import { extractErrorMessage, request } from "api";
import withScreenSecurity from "component/withScreenSecurity";
import React, { useEffect, useState, useCallback } from "react";
import "react-draft-wysiwyg/dist/react-draft-wysiwyg.css";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router";
import FileUploadZone from "utils/FileUpload/FileUploadZone";
import { randomImageName } from "utils/FileUpload/covert";
import { errorNoti, successNoti } from "utils/notification";
import HustCodeEditor from "../../common/HustCodeEditor";
import HustDropzoneArea from "../../common/HustDropzoneArea";
import RichTextEditor from "../../common/editor/RichTextEditor";
import { CompileStatus } from "./CompileStatus";
import { COMPUTER_LANGUAGES, CUSTOM_EVALUATION, NORMAL_EVALUATION } from "./Constant";
import ListTestCase from "./ListTestCase";
import ModelAddNewTag from "./ModelAddNewTag";
import { getAllTags } from "./service/TagService";
import ProgrammingContestLayout from "./ProgrammingContestLayout";
import { useHistory } from "react-router-dom";
import StyledSelect from "../../select/StyledSelect";
import { getLevels, getPublicOptions, getStatuses } from "./CreateProblem";
import FilterByTag from "../../table/FilterByTag";
import TertiaryButton from "../../button/TertiaryButton";
import AddIcon from "@mui/icons-material/Add";
import ArrowUpwardIcon from "@mui/icons-material/ArrowUpward";
import ArrowDownwardIcon from "@mui/icons-material/ArrowDownward";
import AddCircleOutlineIcon from "@mui/icons-material/AddCircleOutline";
import DeleteIcon from "@mui/icons-material/Delete";
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

const useStyles = makeStyles((theme) => ({
  description: {
    marginTop: theme.spacing(3),
    marginBottom: theme.spacing(3),
  },
  blockCodeContainer: {
    display: "flex",
    marginTop: theme.spacing(2),
    marginBottom: theme.spacing(2),
  },
  codeEditorWrapper: {
    width: "75%",
  },
  blockCodeControls: {
    width: "25%",
    paddingLeft: theme.spacing(1),
    display: "flex",
    flexDirection: "column",
    justifyContent: "flex-start",
    alignItems: "flex-start",
    gap: theme.spacing(1),
  },
  disabledBlock: {
    opacity: 0.6,
    pointerEvents: "none",
  },
  controlButtons: {
    display: "flex",
    gap: theme.spacing(0.5),
  },
  expandIcon: {
    transition: theme.transitions.create('transform', {
      duration: theme.transitions.duration.shortest,
    }),
  },
  expandIconOpen: {
    transform: 'rotate(180deg)',
  },
}));

const PROGRAMMING_LANGUAGES = Object.keys(COMPUTER_LANGUAGES).map((key) => ({
  label: key,
  value: COMPUTER_LANGUAGES[key],
}));

// Custom debounce function
const debounce = (func, wait) => {
  let timeout;
  return (...args) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
};

function EditProblem() {
  const history = useHistory();
  const { problemId } = useParams();
  const classes = useStyles();
  const { t } = useTranslation([
    "education/programmingcontest/problem",
    "common",
    "validation",
  ]);
  const levels = getLevels(t);
  const publicOptions = getPublicOptions(t);
  const statuses = getStatuses(t);

  const [problemName, setProblemName] = useState("");
  const [description, setDescription] = useState("");
  const [solution, setSolution] = useState("");
  const [timeLimitCPP, setTimeLimitCPP] = useState('');
  const [timeLimitJAVA, setTimeLimitJAVA] = useState('');
  const [timeLimitPYTHON, setTimeLimitPYTHON] = useState('');
  const [memoryLimit, setMemoryLimit] = useState('');
  const [levelId, setLevelId] = useState("");
  const [codeSolution, setCodeSolution] = useState("");
  const [isPreloadCode, setIsPreloadCode] = useState(false);
  const [preloadCode, setPreloadCode] = useState("");
  const [solutionCheckerLanguage, setSolutionCheckerLanguage] = useState(COMPUTER_LANGUAGES.CPP17);
  const [solutionChecker, setSolutionChecker] = useState("");
  const [isCustomEvaluated, setIsCustomEvaluated] = useState(false);
  const [languageSolution, setLanguageSolution] = useState(COMPUTER_LANGUAGES.CPP17);
  const [showCompile, setShowCompile] = useState(false);
  const [statusSuccessful, setStatusSuccessful] = useState(false);
  const [isPublic, setIsPublic] = useState('');
  const [compileMessage, setCompileMessage] = useState("");
  const [tags, setTags] = useState([]);
  const [selectedTags, setSelectedTags] = useState([]);
  const [attachmentFiles, setAttachmentFiles] = useState([]);
  const [fetchedImageArray, setFetchedImageArray] = useState([]);
  const [removedFilesId, setRemovedFileIds] = useState([]);
  const [status, setStatus] = useState('');
  const [isOwner, setIsOwner] = useState(false);
  const [sampleTestCase, setSampleTestCase] = useState(null);
  const [problem, setProblem] = useState({});
  const [canEditBlocks, setCanEditBlocks] = useState(false);
  const [loading, setLoading] = useState(false);
  const [openModalAddNewTag, setOpenModalAddNewTag] = useState(false);
  const [isProblemBlock, setIsProblemBlock] = useState(false);
  const [blockCodes, setBlockCodes] = useState(
    Object.fromEntries(PROGRAMMING_LANGUAGES.map(({ value }) => [value, []]))
  );
  const [selectedLanguage, setSelectedLanguage] = useState(COMPUTER_LANGUAGES.CPP17);
  const [isBlockCodesExpanded, setIsBlockCodesExpanded] = useState(false);

  const handleGetTagsSuccess = (res) => setTags(res.data);

  const handleSelectTags = (tags) => {
    setSelectedTags(tags);
  };

  const handleAttachmentFiles = (files) => {
    setAttachmentFiles(files);
  };

  const handleDeleteImageAttachment = async (fileId) => {
    setFetchedImageArray(
      fetchedImageArray.filter((file) => file.fileName !== fileId)
    );
    setRemovedFileIds([...removedFilesId, fileId]);
  };

  const checkCompile = () => {
    let body = {
      source: codeSolution,
      computerLanguage: languageSolution,
    };

    setShowCompile(false);
    setLoading(true);
    request(
      "post",
      "/check-compile",
      (res) => {
        setLoading(false);
        setShowCompile(true);
        setStatusSuccessful(res.data.status !== "Compilation Error");
        setCompileMessage(res.data);
      },
      {
        onError: (e) => {
          setLoading(false);
          errorNoti(extractErrorMessage(e) || t("common:error"), 3000);
        }
      },
      body
    );
  };

  const validateSubmit = () => {
    if (problemName === "") {
      errorNoti(
        t("validation:missingField", { fieldName: t("problemName") }),
        3000
      );
      return false;
    }
    if (timeLimitCPP < 1 || timeLimitJAVA < 1 || timeLimitPYTHON < 1 ||
        timeLimitCPP > 300 || timeLimitJAVA > 300 || timeLimitPYTHON > 300) {
      errorNoti(
        t("validation:numberBetween", {
          fieldName: t("timeLimit"),
          min: 1,
          max: 300,
        }),
        3000
      );
      return false;
    }
    if (memoryLimit < 3 || memoryLimit > 1024) {
      errorNoti(
        t("validation:numberBetween", {
          fieldName: t("memoryLimit"),
          min: 3,
          max: 1024,
        }),
        3000
      );
      return false;
    }
    if (problem.correctSolutionSourceCode !== codeSolution && !statusSuccessful) {
      errorNoti(t("validateSubmit.warningCheckSolutionCompile"), 5000);
      return false;
    }
    if (isProblemBlock && Object.values(blockCodes).every((blocks) => blocks.length === 0)) {
      errorNoti(t("validateSubmit.noBlockCodesAdded"), 5000);
      return false;
    }
    return true;
  };

  const handleCopyAllCode = async () => {
    const blocks = blockCodes[selectedLanguage] || [];
    if (blocks.length === 0) {
      errorNoti(t("noBlockCodesToCopy"), 3000);
      return;
    }
    const allCode = blocks.map(block => block.code).join('\n\n');
    await navigator.clipboard.writeText(allCode);
  };

  function handleSubmit() {
    if (!validateSubmit()) return;

    setLoading(true);
    const tagIds = selectedTags.map((tag) => tag.tagId);

    let fileId = [];
    if (attachmentFiles.length > 0) {
      fileId = attachmentFiles.map((file) => {
        if (typeof file.name !== "undefined") {
          return file.name;
        }
        if (typeof file.fileName !== "undefined") {
          return file.fileName;
        }
        return file.id;
      });
    }

    let formattedBlockCodes = [];
    if (isProblemBlock) {
      formattedBlockCodes = Object.keys(blockCodes)
        .filter((language) => blockCodes[language].length > 0)
        .flatMap((language) =>
          blockCodes[language].map((block

, index) => ({
            id: block.id || `${language}_${index}`,
            code: block.code,
            forStudent: block.forStudent ? 1 : 0,
            seq: block.seq || index + 1,
            language: language,
          }))
        );
    }

    const body = {
      problemName: problemName,
      problemDescription: description,
      timeLimitCPP: timeLimitCPP,
      timeLimitJAVA: timeLimitJAVA,
      timeLimitPYTHON: timeLimitPYTHON,
      levelId: levelId,
      memoryLimit: memoryLimit,
      correctSolutionLanguage: languageSolution,
      solution: solution,
      correctSolutionSourceCode: codeSolution,
      isPreloadCode: isPreloadCode,
      preloadCode: preloadCode,
      solutionChecker: solutionChecker,
      solutionCheckerLanguage: solutionCheckerLanguage,
      isPublic: isPublic === 'Y',
      fileId: fileId,
      removedFilesId: removedFilesId,
      scoreEvaluationType: isCustomEvaluated ? CUSTOM_EVALUATION : NORMAL_EVALUATION,
      tagIds: tagIds,
      status: status,
      sampleTestCase: sampleTestCase,
      categoryId: isProblemBlock ? 1 : 0, 
      blockCodes: isProblemBlock ? formattedBlockCodes : [], 
    };

    const formData = new FormData();
    formData.append("dto", new Blob([JSON.stringify(body)], { type: 'application/json' }));

    for (const file of attachmentFiles) {
      formData.append("files", file);
    }

    const config = {
      headers: {
        "content-type": "multipart/form-data",
      },
    };

    request(
      "put",
      "/problems/" + problemId,
      (res) => {
        setLoading(false);
        successNoti(t("common:editSuccess", { name: t("problem") }), 3000);
        history.push("/programming-contest/manager-view-problem-detail/" + problemId);
      },
      {
        onError: (e) => {
          setLoading(false);
          errorNoti(extractErrorMessage(e) || t("common:error"), 3000);
        },
      },
      formData,
      config
    );
  }

  const handleBackToList = () => {
    history.push(`/programming-contest/list-problems`);
  };

  const handleExit = () => {
    history.push(`/programming-contest/manager-view-problem-detail/` + problemId);
  };

  const handleTabChange = (event, newValue) => {
    setSelectedLanguage(newValue);
  };

  const handleDeleteBlock = (index) => {
    if (!canEditBlocks) {
      errorNoti(t("noPermissionToEditBlocks"), 3000);
      return;
    }
    setBlockCodes((prev) => ({
      ...prev,
      [selectedLanguage]: prev[selectedLanguage].filter((_, i) => i !== index),
    }));
  };

  const handleMoveUp = useCallback((index) => {
    if (!canEditBlocks) {
      errorNoti(t("noPermissionToEditBlocks"), 3000);
      return;
    }
    if (index === 0) return;
    setBlockCodes((prev) => {
      const newBlocks = [...prev[selectedLanguage]];
      [newBlocks[index - 1], newBlocks[index]] = [newBlocks[index], newBlocks[index - 1]];
      const updatedBlocks = newBlocks.map((block, i) => ({
        ...block,
        seq: i + 1, 
      }));
      return { ...prev, [selectedLanguage]: updatedBlocks };
    });
  }, [canEditBlocks, selectedLanguage]);

  const handleMoveDown = useCallback((index) => {
    if (!canEditBlocks) {
      errorNoti(t("noPermissionToEditBlocks"), 3000);
      return;
    }
    if (index === blockCodes[selectedLanguage].length - 1) return;
    setBlockCodes((prev) => {
      const newBlocks = [...prev[selectedLanguage]];
      [newBlocks[index], newBlocks[index + 1]] = [newBlocks[index + 1], newBlocks[index]];
      const updatedBlocks = newBlocks.map((block, i) => ({
        ...block,
        seq: i + 1, 
      }));
      return { ...prev, [selectedLanguage]: updatedBlocks };
    });
  }, [canEditBlocks, selectedLanguage, blockCodes]);

  const debouncedMoveUp = useCallback(debounce((index) => handleMoveUp(index), 300), [handleMoveUp]);
  const debouncedMoveDown = useCallback(debounce((index) => handleMoveDown(index), 300), [handleMoveDown]);

  const handleInsertAbove = (index) => {
    if (!canEditBlocks) {
      errorNoti(t("noPermissionToEditBlocks"), 3000);
      return;
    }
    setBlockCodes((prev) => {
      const newBlocks = [...prev[selectedLanguage]];
      newBlocks.splice(index, 0, {
        code: "// Write your code here",
        forStudent: 0,
        seq: index,
        id: `${selectedLanguage}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`, // Unique ID
      });
      return {
        ...prev,
        [selectedLanguage]: newBlocks.map((block, i) => ({ ...block, seq: i + 1 })),
      };
    });
  };

  const handleInsertBelow = (index) => {
    if (!canEditBlocks) {
      errorNoti(t("noPermissionToEditBlocks"), 3000);
      return;
    }
    setBlockCodes((prev) => {
      const newBlocks = [...prev[selectedLanguage]];
      newBlocks.splice(index + 1, 0, {
        code: "// Write your code here",
        forStudent: 0,
        seq: index + 2,
        id: `${selectedLanguage}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`, // Unique ID
      });
      return {
        ...prev,
        [selectedLanguage]: newBlocks.map((block, i) => ({ ...block, seq: i + 1 })),
      };
    });
  };

  const handleAddBlock = () => {
    if (!canEditBlocks) {
      errorNoti(t("noPermissionToEditBlocks"), 3000);
      return;
    }
    try {
      const language = selectedLanguage || COMPUTER_LANGUAGES.CPP17;
      setBlockCodes((prev) => ({
        ...prev,
        [language]: [
          ...(prev[language] || []),
          {
            code: "// Write your code here",
            forStudent: 0,
            seq: prev[language].length + 1,
            id: `${language}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`, // Unique ID
          },
        ],
      }));
    } catch (error) {
      console.error("Error adding block code:", error);
      errorNoti(t("Failed to add block code"), 3000);
    }
  };

  // Memoized handler for code changes
  const handleCodeChange = useCallback((newCode, index) => {
    if (!canEditBlocks) {
      errorNoti(t("noPermissionToEditBlocks"), 3000);
      return;
    }
    try {
      setBlockCodes((prev) => ({
        ...prev,
        [selectedLanguage]: prev[selectedLanguage].map((b, i) =>
          i === index ? { ...b, code: newCode } : b
        ),
      }));
    } catch (error) {
      console.error("Error updating code:", error);
      errorNoti(t("Failed to update code"), 3000);
    }
  }, [canEditBlocks, selectedLanguage, t]);

  // Handle isProblemBlock checkbox change
  const handleProblemBlockChange = () => {
    setIsProblemBlock((prev) => !prev);
  };

  useEffect(() => {
    request(
      "get",
      "teacher/problems/" + problemId,
      (res) => {
        const data = res.data;
        setProblem(data);

        if (data.attachment && data.attachment.length !== 0) {
          const newFileURLArray = data.attachment.map((url) => ({
            id: randomImageName(),
            content: url,
          }));
          newFileURLArray.forEach((file, idx) => {
            file.fileName = data.attachmentNames[idx];
          });
          setFetchedImageArray(newFileURLArray);
        }

        setProblemName(data.problemName);
        setLevelId(data.levelId);
        setTimeLimitCPP(data.timeLimitCPP);
        setTimeLimitJAVA(data.timeLimitJAVA);
        setTimeLimitPYTHON(data.timeLimitPYTHON);
        setMemoryLimit(data.memoryLimit);
        setIsPublic(data.publicProblem ? 'Y' : 'N');
        setLanguageSolution(data.correctSolutionLanguage);
        setCodeSolution(data.correctSolutionSourceCode);
        setIsPreloadCode(data.isPreloadCode);
        setPreloadCode(data.preloadCode);
        setSolutionCheckerLanguage(data.solutionCheckerSourceLanguage);
        setSolutionChecker(data.solutionCheckerSourceCode || "");
        setIsCustomEvaluated(data.scoreEvaluationType === CUSTOM_EVALUATION);
        setDescription(data.problemDescription);
        setSelectedTags(data.tags);
        setStatus(data.statusId);
        setSampleTestCase(data.sampleTestCase);
        setIsOwner(data.roles?.includes("OWNER"));
        setCanEditBlocks(data.canEditBlocks || false);
        setIsProblemBlock(data.categoryId > 0); // Initialize based on categoryId

        if (data.categoryId > 0) {
          const newBlockCodes = Object.fromEntries(
            PROGRAMMING_LANGUAGES.map(({ value }) => [value, []])
          );
          data.blockCodes.forEach((block) => {
            if (newBlockCodes[block.language]) {
              newBlockCodes[block.language].push({
                id: block.id,
                code: block.code,
                forStudent: block.forStudent,
                seq: block.seq,
              });
            }
          });
          setBlockCodes(newBlockCodes);
        }
      },
      {
        onError: (e) => {
          errorNoti(extractErrorMessage(e) || t("common:error"), 3000);
        },
      }
    );
  }, [problemId, t]);

  useEffect(() => {
    getAllTags(handleGetTagsSuccess);
  }, []);

  return (
    <ProgrammingContestLayout title={t("common:edit", { name: t("problem") })} onBack={handleBackToList}>
      <Typography variant="h6">
        {t("generalInfo")}
      </Typography>

      <Grid container spacing={2} mt={0}>
        <Grid item xs={3}>
          <TextField
            fullWidth
            size='small'
            required
            id="problemName"
            label={t("problemName")}
            value={problemName}
            onChange={(event) => {
              setProblemName(event.target.value);
            }}
          />
        </Grid>

        <Grid item xs={3}>
          <StyledSelect
            fullWidth
            required
            key={t("level")}
            label={t("level")}
            options={levels}
            value={levelId}
            sx={{ minWidth: 'unset', mr: 'unset' }}
            onChange={(event) => {
              setLevelId(event.target.value);
            }}
          />
        </Grid>

        <Grid item xs={3}>
          <StyledSelect
            fullWidth
            required
            key={t("status")}
            label={t("status")}
            options={statuses}
            value={status}
            sx={{ minWidth: 'unset', mr: 'unset' }}
            onChange={(event) => {
              setStatus(event.target.value);
            }}
            disabled={!isOwner}
          />
        </Grid>

        <Grid item xs={3}>
          <StyledSelect
            fullWidth
            required
            key={t("common:public")}
            label={t("common:public")}
            options={publicOptions}
            sx={{ minWidth: 'unset', mr: 'unset' }}
            value={isPublic}
            onChange={(event) => {
              setIsPublic(event.target.value);
            }}
          />
        </Grid>

        <Grid item xs={3}>
          <TextField
            fullWidth
            size='small'
            required
            id="timeLimitCPP"
            label={t("timeLimit") + ' C/CPP'}
            type="number"
            value={timeLimitCPP}
            onChange={(event) => {
              setTimeLimitCPP(event.target.value);
            }}
            InputProps={{
              endAdornment: <InputAdornment position="end">s</InputAdornment>,
            }}
          />
        </Grid>

        <Grid item xs={3}>
          <TextField
            fullWidth
            size='small'
            required
            id="timeLimitJAVA"
            label={t("timeLimit") + ' Java'}
            type="number"
            value={timeLimitJAVA}
            onChange={(event) => {
              setTimeLimitJAVA(event.target.value);
            }}
            InputProps={{
              endAdornment: <InputAdornment position="end">s</InputAdornment>,
            }}
          />
        </Grid>

        <Grid item xs={3}>
          <TextField
            fullWidth
            size='small'
            required
            id="timeLimitPYTHON"
            label={t("timeLimit") + ' Python'}
            type="number"
            value={timeLimitPYTHON}
            onChange={(event) => {
              setTimeLimitPYTHON(event.target.value);
            }}
            InputProps={{
              endAdornment: <InputAdornment position="end">s</InputAdornment>,
            }}
          />
        </Grid>

        <Grid item xs={3}>
          <TextField
            fullWidth
            size='small'
            required
            id="memoryLimit"
            label={t("memoryLimit")}
            type="number"
            value={memoryLimit}
            onChange={(event) => {
              setMemoryLimit(event.target.value);
            }}
            InputProps={{
              endAdornment: <InputAdornment position="end">MB</InputAdornment>,
            }}
          />
        </Grid>

        <Grid item xs={9}>
          <FilterByTag limitTags={3} tags={tags} onSelect={handleSelectTags} value={selectedTags} />
        </Grid>
        <Grid item xs={3}>
          <TertiaryButton
            startIcon={<AddIcon />}
            onClick={() => setOpenModalAddNewTag(true)}
          >
            {t("common:add", { name: t('tag') })}
          </TertiaryButton>
        </Grid>
        <Grid item xs={3}>
          <FormControlLabel
            label={t("problemBlock")}
            control={
              <Checkbox
                checked={isProblemBlock}
                onChange={handleProblemBlockChange} // Use custom handler
                disabled={!canEditBlocks}
              />
            }
          />
        </Grid>
      </Grid>

      <Box className={classes.description}>
        <Typography
          variant="h6"
          sx={{ marginTop: "8px", marginBottom: "8px" }}
        >
          {t("problemDescription")}
        </Typography>
        <RichTextEditor
          content={description}
          onContentChange={(text) => setDescription(text)}
        />
        <HustCodeEditor
          title={t("sampleTestCase")}
          placeholder={null}
          sourceCode={sampleTestCase}
          onChangeSourceCode={(code) => {
            setSampleTestCase(code);
          }}
        />

        <HustDropzoneArea
          onChangeAttachment={(files) => handleAttachmentFiles(files)}
        />
        {isProblemBlock && (
          <>
            <Box sx={{ display: 'flex', alignItems: 'center', marginTop: '12px' }}>
              <IconButton
                onClick={() => setIsBlockCodesExpanded(!isBlockCodesExpanded)}
                aria-expanded={isBlockCodesExpanded}
                aria-label={t("common:toggleBlockCodes")}
                style={{ color: '#00bcd4' }}
                size="small"
              >
                <ExpandMoreIcon
                  className={`${classes.expandIcon} ${isBlockCodesExpanded ? classes.expandIconOpen : ''}`}
                />
              </IconButton>
              <Typography variant="body1">
                {t("common:toggleBlockCodes")}
              </Typography>
            </Box>
            <Collapse in={isBlockCodesExpanded}>
              <Tabs value={selectedLanguage} onChange={handleTabChange} sx={{ marginTop: "12px" }}>
                {PROGRAMMING_LANGUAGES.map((lang) => (
                  <Tab key={lang.value} label={lang.label} value={lang.value} />
                ))}
              </Tabs>
              
              <Box className={!canEditBlocks ? classes.disabledBlock : undefined}>
                {blockCodes[selectedLanguage].map((block, index) => (
                  <Box className={classes.blockCodeContainer} key={block.id || index}>
                    <Box className={classes.codeEditorWrapper}>
                      <HustCodeEditor
                        sourceCode={block.code || ""}
                        onChangeSourceCode={(newCode) => handleCodeChange(newCode, index)}
                        language={selectedLanguage}
                        height="300px"
                        readOnly={!canEditBlocks}
                        hideProgrammingLanguage={1}
                        blockEditor={1}
                        isStudentBlock={block.forStudent}
                      />
                    </Box>
                    <Box className={classes.blockCodeControls}>
                      <StyledSelect
                        size="small"
                        value={block.forStudent ? "student" : "teacher"}
                        onChange={(event) => {
                          if (!canEditBlocks) {
                            errorNoti(t("noPermissionToEditBlocks"), 3000);
                            return;
                          }
                          setBlockCodes((prev) => ({
                            ...prev,
                            [selectedLanguage]: prev[selectedLanguage].map((b, i) =>
                              i === index ? { ...b, forStudent: event.target.value === "student" } : b
                            ),
                          }));
                        }}
                        options={[
                          { label: t("forTeacher"), value: "teacher" },
                          { label: t("forStudent"), value: "student" },
                        ]}
                        sx={{ width: "250px", mt: 5 }}
                        disabled={!canEditBlocks}
                      />
                      <Box className={classes.controlButtons} sx={{ mt: 1 }}>
                        <IconButton
                          onClick={() => debouncedMoveUp(index)}
                          disabled={!canEditBlocks || index === 0}
                          title={t("moveUp", { ns: "common" })}
                          size="small"
                        >
                          <ArrowUpwardIcon fontSize="small" />
                        </IconButton>
                        <IconButton
                          onClick={() => debouncedMoveDown(index)}
                          disabled={!canEditBlocks || index === blockCodes[selectedLanguage].length - 1}
                          title={t("moveDown", { ns: "common" })}
                          size="small"
                        >
                          <ArrowDownwardIcon fontSize="small" />
                        </IconButton>
                        <IconButton
                          onClick={() => handleInsertAbove(index)}
                          disabled={!canEditBlocks}
                          title={t("insertAbove", { ns: "common" })}
                          size="small"
                        >
                          <AddCircleOutlineIcon fontSize="small" />
                        </IconButton>
                        <IconButton
                          onClick={() => handleInsertBelow(index)}
                          disabled={!canEditBlocks}
                          title={t("insertBelow", { ns: "common" })}
                          size="small"
                        >
                          <AddCircleOutlineIcon fontSize="small" />
                        </IconButton>
                        <IconButton
                          onClick={() => handleDeleteBlock(index)}
                          disabled={!canEditBlocks}
                          title={t("delete", { ns: "common" })}
                          size="small"
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Box>
                    </Box>
                  </Box>
                ))}
              </Box>
            </Collapse>
            <Stack direction="row" spacing={2} sx={{ marginTop: "12px" }}>
              <Button
                variant="outlined"
                onClick={handleAddBlock}
                disabled={!canEditBlocks}
              >
                {t("addProblemBlock")}
              </Button>
              <TertiaryButton
                variant="outlined"
                startIcon={<ContentCopyIcon />}
                onClick={handleCopyAllCode}
                disabled={!(blockCodes[selectedLanguage]?.length > 0)}
              >
                {t("common:copyAllCode")}
              </TertiaryButton>
            </Stack>
          </>
        )}
      </Box>

      {fetchedImageArray.length !== 0 &&
        fetchedImageArray.map((file) => (
          <FileUploadZone
            key={file.id}
            file={file}
            removable={true}
            onRemove={() => handleDeleteImageAttachment(file.fileName)}
          />
        ))}

      <Box sx={{ marginTop: "32px" }} />
      <HustCodeEditor
        title={t("solutionSourceCode") + " *"}
        language={languageSolution}
        onChangeLanguage={(event) => {
          setLanguageSolution(event.target.value);
        }}
        sourceCode={codeSolution}
        onChangeSourceCode={(code) => {
          setCodeSolution(code);
        }}
      />

      <LoadingButton
        variant="outlined"
        loading={loading}
        onClick={checkCompile}
        sx={{ margin: "12px 0", textTransform: 'none' }}
      >
        {t("checkSolutionCompile")}
      </LoadingButton>

      <CompileStatus
        showCompile={showCompile}
        statusSuccessful={statusSuccessful}
        detail={compileMessage}
      />

      <Box sx={{ marginTop: "12px" }}>
        <FormControlLabel
          label={t("isPreloadCode")}
          control={
            <Checkbox
              checked={isPreloadCode}
              onChange={() => setIsPreloadCode(!isPreloadCode)}
            />
          }
        />
        {isPreloadCode && (
          <HustCodeEditor
            title={t("preloadCode")}
            sourceCode={preloadCode}
            onChangeSourceCode={(code) => {
              setPreloadCode(code);
            }}
            height="280px"
            placeholder="Write the initial code segment that provided to the participants here"
          />
        )}
      </Box>

      <Box sx={{ marginTop: "12px" }}>
        <FormControlLabel
          label={t("isCustomEvaluated")}
          control={
            <Checkbox
              checked={isCustomEvaluated}
              onChange={() => setIsCustomEvaluated(!isCustomEvaluated)}
            />
          }
        />
        <Typography variant="body2" color="gray">
          {t("customEvaluationNote1")}
        </Typography>

        {isCustomEvaluated && (
          <HustCodeEditor
            title={t("checkerSourceCode")}
            language={solutionCheckerLanguage}
            onChangeLanguage={(event) => {
              setSolutionCheckerLanguage(event.target.value);
            }}
            sourceCode={solutionChecker}
            onChangeSourceCode={(code) => {
              setSolutionChecker(code);
            }}
            placeholder={t("checkerSourceCodePlaceholder")}
          />
        )}
      </Box>

      <ListTestCase />

      <Stack direction="row" spacing={2} mt={2}>
        <TertiaryButton variant="outlined" onClick={handleExit}>
          {t("common:exit")}
        </TertiaryButton>
        <LoadingButton
          variant="contained"
          loading={loading}
          onClick={handleSubmit}
          sx={{ textTransform: 'capitalize' }}
        >
          {t("save", { ns: "common" })}
        </LoadingButton>
      </Stack>

      <ModelAddNewTag
        isOpen={openModalAddNewTag}
        handleSuccess={() => {
          successNoti(t("common:addSuccess", { name: t('tag') }), 3000);
          getAllTags(handleGetTagsSuccess);
        }}
        handleClose={() => setOpenModalAddNewTag(false)}
      />
    </ProgrammingContestLayout>
  );
}

const screenName = "SCR_EDIT_PROBLEM";
export default withScreenSecurity(EditProblem, screenName, true);