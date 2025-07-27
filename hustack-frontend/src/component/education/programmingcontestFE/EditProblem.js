import {LoadingButton} from "@mui/lab";
import {
  Box,
  Checkbox,
  Collapse,
  FormControlLabel,
  Grid,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import {debounce} from "lodash";
import {extractErrorMessage, request, saveFile} from "../../../api";
import withScreenSecurity from "component/withScreenSecurity";
import React, {useCallback, useEffect, useMemo, useState} from "react";
import "react-draft-wysiwyg/dist/react-draft-wysiwyg.css";
import {useTranslation} from "react-i18next";
import {useParams} from "react-router";
import FileUploadZone from "utils/FileUpload/FileUploadZone";
import HustDropzoneArea from "../../common/HustDropzoneArea";
import {errorNoti, successNoti} from "utils/notification";
import HustCodeEditor from "../../common/HustCodeEditor";
import HustCopyCodeBlock from "../../common/HustCopyCodeBlock";
import RichTextEditor from "../../common/editor/RichTextEditor";
import {CompileStatus} from "./CompileStatus";
import {
  COMPUTER_LANGUAGES,
  CUSTOM_EVALUATION,
  mapLanguageToCodeBlockLanguage,
  mapLanguageToDisplayName,
  NORMAL_EVALUATION
} from "./Constant";
import ListTestCase from "./ListTestCase";
import ModelAddNewTag from "./ModelAddNewTag";
import {getAllTags} from "./service/TagService";
import ProgrammingContestLayout from "./ProgrammingContestLayout";
import {useHistory} from "react-router-dom";
import StyledSelect from "../../select/StyledSelect";
import {getLevels, getPublicOptions, getStatuses} from "./CreateProblem";
import FilterByTag from "../../table/FilterByTag";
import TertiaryButton from "../../button/TertiaryButton";
import AddIcon from "@mui/icons-material/Add";
import ArrowUpwardIcon from "@mui/icons-material/ArrowUpward";
import ArrowDownwardIcon from "@mui/icons-material/ArrowDownward";
import DeleteIcon from "@mui/icons-material/Delete";
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import {v4 as uuidv4} from 'uuid';
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown';
import {AntTab, AntTabs} from "component/tab";
import KeyboardDoubleArrowUpIcon from '@mui/icons-material/KeyboardDoubleArrowUp';
import KeyboardDoubleArrowDownIcon from '@mui/icons-material/KeyboardDoubleArrowDown';
import FormatListBulletedRoundedIcon from '@mui/icons-material/FormatListBulletedRounded';
import ArticleRoundedIcon from '@mui/icons-material/ArticleRounded';
import {dracula, github} from 'react-code-blocks';
import {grey} from '@mui/material/colors';
import RotatingIconButton from "../../common/RotatingIconButton";

const PROGRAMMING_LANGUAGES = Object.keys(COMPUTER_LANGUAGES).map((key) => ({
  label: key,
  value: COMPUTER_LANGUAGES[key],
}));


function EditProblem() {
  const history = useHistory();
  const {problemId} = useParams();
  const {t} = useTranslation([
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
  const [fetchedImageArray, setFetchedImageArray] = useState([]);
  const [attachmentFiles, setAttachmentFiles] = useState([]);
  const [dropzoneFiles, setDropzoneFiles] = useState([]);
  const [removedFilesId, setRemovedFileIds] = useState([]);
  const [status, setStatus] = useState('');
  const [isOwner, setIsOwner] = useState(false);
  const [sampleTestCase, setSampleTestCase] = useState(null);
  const [problem, setProblem] = useState({});
  const [canEditBlocks, setCanEditBlocks] = useState(undefined);
  const [loading, setLoading] = useState(false);
  const [openModalAddNewTag, setOpenModalAddNewTag] = useState(false);
  const [isProblemBlock, setIsProblemBlock] = useState(false);
  const [blockCodes, setBlockCodes] = useState(
    Object.fromEntries(PROGRAMMING_LANGUAGES.map(({value}) => [value, []]))
  );
  const [selectedLanguage, setSelectedLanguage] = useState(COMPUTER_LANGUAGES.CPP17);
  const [isBlockCodesExpanded, setIsBlockCodesExpanded] = useState(false);
  const [rotationCount, setRotationCount] = useState(0);
  const [blockDisplayMode, setBlockDisplayMode] = useState("individual");

  const handleGetTagsSuccess = (res) => setTags(res.data);

  const handleSelectTags = (tags) => {
    setSelectedTags(tags);
  };

  const handleAttachmentFiles = (files) => {
    console.log('handleAttachmentFiles called with:', files);
    setAttachmentFiles(files);
  };

  const handleDeleteImageAttachment = async (fileId) => {
    console.log('handleDeleteImageAttachment called with fileId:', fileId);
    const fileToDelete = fetchedImageArray.find(file => file.id === fileId);
    console.log('fileToDelete:', fileToDelete);

    // Only handle existing files from API
    if (fileToDelete) {
      setFetchedImageArray(
        fetchedImageArray.filter((file) => file.id !== fileId)
      );
      setRemovedFileIds([...removedFilesId, fileToDelete.id]); // Gửi fileId thay vì fileName
    }
  };

  const handleDownloadFile = (file) => {
    request(
      "GET",
      `/problems/${problemId}/attachments/${file.id}`,
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
        t("validation:missingField", {fieldName: t("problemName")}),
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
      errorNoti(t("common:noBlockCodesAdded"), 5000);
      return false;
    }
    return true;
  };

  const handleCopyAllCode = async () => {
    const blocks = blockCodes[selectedLanguage] || [];
    if (blocks.length === 0) {
      errorNoti(t("common:noBlockCodesToCopy"), 3000);
      return;
    }
    const allCode = blocks.map(block => block.code).join('\n');
    await navigator.clipboard.writeText(allCode).then(() => {
      successNoti(t('common:copySuccess'), 2000);
    });
    ;
  };

  function handleSubmit() {
    if (!validateSubmit()) return;

    setLoading(true);
    const tagIds = selectedTags.map((tag) => tag.tagId);

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

    // Get new files (without id) and removed file ids
    const newFiles = attachmentFiles;
    const removedFileIds = removedFilesId;
    // Backend sẽ tự động tạo unique filename, không cần UUID ở frontend
    const fileIds = newFiles.map((file) => file.name);

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
      removedFilesId: removedFileIds,
      fileId: fileIds, // Add fileId array
      scoreEvaluationType: isCustomEvaluated ? CUSTOM_EVALUATION : NORMAL_EVALUATION,
      tagIds: tagIds,
      status: status,
      sampleTestCase: sampleTestCase,
      categoryId: isProblemBlock ? 1 : 0,
      blockCodes: isProblemBlock ? formattedBlockCodes : [],
    };

    const formData = new FormData();
    formData.append("dto", new Blob([JSON.stringify(body)], {type: 'application/json'}));

    for (const file of newFiles) {
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
        successNoti(t("common:editSuccess", {name: t("problem")}), 3000);
        history.push("/programming-contest/manager-view-problem-detail/" + problemId);
      },
      {
        onError: (e) => {
          setLoading(false);
          if (!(e.response && e.response.status === 403)) {
            errorNoti(t("common:error"), 3000);
          }
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
      errorNoti(t("common:noPermissionToEditBlocks"), 3000);
      return;
    }
    setBlockCodes((prev) => ({
      ...prev,
      [selectedLanguage]: prev[selectedLanguage].filter((_, i) => i !== index),
    }));
  };

  const handleMoveUp = useCallback((index) => {
    if (!canEditBlocks) {
      errorNoti(t("common:noPermissionToEditBlocks"), 3000);
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
      return {...prev, [selectedLanguage]: updatedBlocks};
    });
  }, [canEditBlocks, selectedLanguage]);

  const handleMoveDown = useCallback((index) => {
    if (!canEditBlocks) {
      errorNoti(t("common:noPermissionToEditBlocks"), 3000);
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
      return {...prev, [selectedLanguage]: updatedBlocks};
    });
  }, [canEditBlocks, selectedLanguage, blockCodes]);

  // Create stable debounced functions
  const debouncedMoveUp = useMemo(() => debounce((index) => handleMoveUp(index), 300), [handleMoveUp]);
  const debouncedMoveDown = useMemo(() => debounce((index) => handleMoveDown(index), 300), [handleMoveDown]);
  const debouncedSolutionCodeChange = useMemo(() => debounce((code) => setCodeSolution(code), 300), []);
  const debouncedPreloadCodeChange = useMemo(() => debounce((code) => setPreloadCode(code), 300), []);
  const debouncedCheckerCodeChange = useMemo(() => debounce((code) => setSolutionChecker(code), 300), []);

  const handleInsertAbove = (index) => {
    if (!canEditBlocks) {
      errorNoti(t("common:noPermissionToEditBlocks"), 3000);
      return;
    }
    setBlockCodes((prev) => {
      const newBlocks = [...prev[selectedLanguage]];
      newBlocks.splice(index, 0, {
        code: null,
        forStudent: 0,
        seq: index,
        id: `${selectedLanguage}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`, // Unique ID
      });
      return {
        ...prev,
        [selectedLanguage]: newBlocks.map((block, i) => ({...block, seq: i + 1})),
      };
    });
  };

  const handleInsertBelow = (index) => {
    if (!canEditBlocks) {
      errorNoti(t("common:noPermissionToEditBlocks"), 3000);
      return;
    }
    setBlockCodes((prev) => {
      const newBlocks = [...prev[selectedLanguage]];
      newBlocks.splice(index + 1, 0, {
        code: null,
        forStudent: 0,
        seq: index + 2,
        id: `${selectedLanguage}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`, // Unique ID
      });
      return {
        ...prev,
        [selectedLanguage]: newBlocks.map((block, i) => ({...block, seq: i + 1})),
      };
    });
  };

  const handleAddBlock = () => {
    if (!canEditBlocks) {
      errorNoti(t("common:noPermissionToEditBlocks"), 3000);
      return;
    }
    try {
      const language = selectedLanguage || COMPUTER_LANGUAGES.CPP17;
      setBlockCodes((prev) => ({
        ...prev,
        [language]: [
          ...(prev[language] || []),
          {
            code: null,
            forStudent: 0,
            seq: prev[language].length + 1,
            id: `${language}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`, // Unique ID
          },
        ],
      }));
    } catch (error) {
      console.error("Error adding block code:", error);
      errorNoti(t("common:failedToAddBlockCode"), 3000);
    }
  };

  // Memoized handler for code changes
  const handleCodeChange = useCallback((newCode, index) => {
    if (!canEditBlocks) {
      errorNoti(t("common:noPermissionToEditBlocks"), 3000);
      return;
    }
    try {
      setBlockCodes((prev) => ({
        ...prev,
        [selectedLanguage]: prev[selectedLanguage].map((b, i) =>
          i === index ? {...b, code: newCode} : b
        ),
      }));
    } catch (error) {
      console.error("Error updating code:", error);
      errorNoti(t("common:failedToUpdateCode"), 3000);
    }
  }, [canEditBlocks, selectedLanguage, t]);

  // Debounced version of handleCodeChange
  const debouncedCodeChange = useMemo(() => debounce((newCode, index) => handleCodeChange(newCode, index), 300), [handleCodeChange]);

  // Handle isProblemBlock checkbox change
  const handleProblemBlockChange = () => {
    setIsProblemBlock((prev) => {
      const newValue = !prev;
      // If enabling problem block, select first language with block codes or CPP17 as default
      if (newValue && !prev) {
        const firstLanguageWithBlocks = PROGRAMMING_LANGUAGES.find(lang =>
          blockCodes[lang.value] && blockCodes[lang.value].length > 0
        );
        setSelectedLanguage(firstLanguageWithBlocks ? firstLanguageWithBlocks.value : COMPUTER_LANGUAGES.CPP17);
      }
      // If disabling problem block, reset to CPP17
      if (!newValue && prev) {
        setSelectedLanguage(COMPUTER_LANGUAGES.CPP17);
      }
      return newValue;
    });
  };

  const handleToggleBlockDisplayMode = () => {
    setBlockDisplayMode((prev) => (prev === "individual" ? "combined" : "individual"));
  };

  const getCombinedBlockCode = () => {
    if (!blockCodes[selectedLanguage]) return "";

    return blockCodes[selectedLanguage]
      .sort((a, b) => a.seq - b.seq)
      .map(block => block.code)
      .join("\n");
  };

  useEffect(() => {
    request(
      "get",
      "teacher/problems/" + problemId,
      (res) => {
        const data = res.data;
        setProblem(data);

        if (data.attachments && data.attachments.length !== 0) {
          setFetchedImageArray(data.attachments);
        }

        // Convert attachments to dropzone format
        if (data.attachments && data.attachments.length > 0) {
          const dropzoneFileObjects = data.attachments.map(attachment => ({
            name: attachment.fileName,
            id: attachment.id,
            // Create a mock File object for dropzone
            size: 0,
            type: 'application/octet-stream',
            lastModified: Date.now(),
          }));
          setDropzoneFiles(dropzoneFileObjects);
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
        setStatus(data.status);
        setSampleTestCase(data.sampleTestCase);
        setIsOwner(data.roles?.includes("OWNER"));
        setCanEditBlocks(data.canEditBlocks || false);
        setIsProblemBlock(data.categoryId > 0); // Initialize based on categoryId

        // If in view mode (canEditBlocks = false), select first language with block codes
        if (!(data.canEditBlocks || false) && data.categoryId > 0) {
          const newBlockCodes = Object.fromEntries(
            PROGRAMMING_LANGUAGES.map(({value}) => [value, []])
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

          const firstLanguageWithBlocks = PROGRAMMING_LANGUAGES.find(lang =>
            newBlockCodes[lang.value] && newBlockCodes[lang.value].length > 0
          );
          if (firstLanguageWithBlocks) {
            setSelectedLanguage(firstLanguageWithBlocks.value);
          }
        }

        if (data.categoryId > 0) {
          const newBlockCodes = Object.fromEntries(
            PROGRAMMING_LANGUAGES.map(({value}) => [value, []])
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

          // Select first language with block codes, or CPP17 as default
          const firstLanguageWithBlocks = PROGRAMMING_LANGUAGES.find(lang =>
            newBlockCodes[lang.value] && newBlockCodes[lang.value].length > 0
          );
          setSelectedLanguage(firstLanguageWithBlocks ? firstLanguageWithBlocks.value : COMPUTER_LANGUAGES.CPP17);
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

  // Select appropriate tab when canEditBlocks changes
  useEffect(() => {
    if (canEditBlocks !== undefined && isProblemBlock) {
      if (canEditBlocks) {
        // In edit mode, select first language with block codes or CPP17 as default
        const firstLanguageWithBlocks = PROGRAMMING_LANGUAGES.find(lang =>
          blockCodes[lang.value] && blockCodes[lang.value].length > 0
        );
        setSelectedLanguage(firstLanguageWithBlocks ? firstLanguageWithBlocks.value : COMPUTER_LANGUAGES.CPP17);
      } else {
        // In view mode, select first language with block codes
        const firstLanguageWithBlocks = PROGRAMMING_LANGUAGES.find(lang =>
          blockCodes[lang.value] && blockCodes[lang.value].length > 0
        );
        if (firstLanguageWithBlocks) {
          setSelectedLanguage(firstLanguageWithBlocks.value);
        }
      }
    }
  }, [canEditBlocks, isProblemBlock]);

  return (
    <ProgrammingContestLayout title={t("common:edit", {name: t("problem")})} onBack={handleBackToList}>
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
            sx={{minWidth: 'unset', mr: 'unset'}}
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
            sx={{minWidth: 'unset', mr: 'unset'}}
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
            sx={{minWidth: 'unset', mr: 'unset'}}
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
          <FilterByTag limitTags={3} tags={tags} onSelect={handleSelectTags} value={selectedTags}/>
        </Grid>
        <Grid item xs={3}>
          <TertiaryButton
            startIcon={<AddIcon/>}
            onClick={() => setOpenModalAddNewTag(true)}
          >
            {t("common:add", {name: t('tag')})}
          </TertiaryButton>
        </Grid>
      </Grid>

      <Box sx={{mt: 3, mb: 2}}>
        <Typography
          variant="h6"
          sx={{marginTop: "8px", marginBottom: "8px"}}
        >
          {t("problemDescription")}
        </Typography>
        <RichTextEditor
          content={description}
          onContentChange={(text) => setDescription(text)}
        />
        <FormControlLabel
          label={t("problemBlock") + (!canEditBlocks ? " " + t("common:alreadyUsed") : "")}
          control={
            <Checkbox
              checked={isProblemBlock}
              onChange={handleProblemBlockChange}
              disabled={!canEditBlocks}
            />
          }
          sx={{mt: 1}}
        />
        {isProblemBlock && canEditBlocks !== undefined && (
          <Box sx={{mt: -3}}>
            <Box sx={{display: 'flex', alignItems: 'center', marginTop: '24px'}}>
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
              {!canEditBlocks && isBlockCodesExpanded && (
                <Box sx={{display: 'flex', alignItems: 'center', ml: 'auto', gap: 1}}>
                  <Tooltip title={t("common:listBlockLayout")}>
                    <IconButton
                      onClick={() => setBlockDisplayMode("individual")}
                      color={blockDisplayMode === "individual" ? "primary" : "default"}
                      size="small"
                    >
                      <FormatListBulletedRoundedIcon/>
                    </IconButton>
                  </Tooltip>
                  <Tooltip title={t("common:combinedBlockLayout")}>
                    <IconButton
                      onClick={() => setBlockDisplayMode("combined")}
                      color={blockDisplayMode === "combined" ? "primary" : "default"}
                      size="small"
                    >
                      <ArticleRoundedIcon/>
                    </IconButton>
                  </Tooltip>
                </Box>
              )}
            </Box>
            <Collapse in={isBlockCodesExpanded}>
              <AntTabs value={selectedLanguage} onChange={handleTabChange} sx={{marginBottom: "12px"}}>
                {(canEditBlocks ? PROGRAMMING_LANGUAGES : PROGRAMMING_LANGUAGES.filter(lang =>
                  blockCodes[lang.value] && blockCodes[lang.value].length > 0
                )).map((lang) => (
                  <AntTab key={lang.value} label={mapLanguageToDisplayName(lang.value)} value={lang.value}
                          sx={{textTransform: 'none'}}/>
                ))}
              </AntTabs>


              {blockDisplayMode === "individual" ? (
                (blockCodes[selectedLanguage] && blockCodes[selectedLanguage].length > 0) ? (
                  blockCodes[selectedLanguage].map((block, index) => (
                    <Box
                      key={block.id || index}
                      sx={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: 2,
                        mb: canEditBlocks ? (index !== blockCodes[selectedLanguage].length - 1 ? 1.5 : 0) : (index === blockCodes[selectedLanguage].length - 1 ? 0 : 1),
                      }}
                    >
                      <Box
                        sx={{
                          width: '48px',
                          minWidth: '48px',
                          display: 'flex',
                          justifyContent: 'center',
                          alignItems: 'flex-start',
                          pt: canEditBlocks ? 0 : '14px',
                        }}
                      >
                        <Typography
                          variant="body2"
                          sx={{
                            color: 'text.secondary',
                            fontWeight: 500,
                          }}
                        >
                          {block.seq || index + 1}
                        </Typography>
                      </Box>
                      <Box sx={{flex: 1}}>
                        {canEditBlocks ? (
                          <HustCodeEditor
                            key={(block.id || uuidv4()) + '_' + block.forStudent}
                            sourceCode={block.code || ""}
                            onChangeSourceCode={(newCode) => debouncedCodeChange(newCode, index)}
                            language={selectedLanguage}
                            height="300px"
                            readOnly={!canEditBlocks}
                            hideProgrammingLanguage={1}
                            theme={block.forStudent ? "github" : "monokai"}
                            minLines={5}
                          />
                        ) : (
                          <>
                            <Typography
                              sx={{
                                position: 'absolute',
                                top: '8px',
                                right: '8px',
                                fontSize: '0.875rem',
                                color: 'text.secondary',
                              }}
                            >
                              {block.forStudent ? t("common:forStudent") : t("common:forTeacher")}
                            </Typography>
                            <Box sx={block.forStudent ? {border: `1px solid ${grey[900]}`, borderRadius: 1} : {}}>
                              <HustCopyCodeBlock
                                text={block.code}
                                language={mapLanguageToCodeBlockLanguage(selectedLanguage)}
                                showLineNumbers
                                isStudentBlock={block.forStudent}
                                theme={block.forStudent ? github : dracula}
                              />
                            </Box>
                          </>
                        )}
                      </Box>
                      {canEditBlocks && (
                        <Box
                          sx={{
                            width: '200px',
                            minWidth: '200px',
                            display: 'flex',
                            flexDirection: 'column',
                            justifyContent: 'center',
                            alignSelf: 'center',
                            gap: 1,
                            pl: 0
                          }}
                        >
                          <StyledSelect
                            size="small"
                            value={block.forStudent ? "student" : "teacher"}
                            onChange={(event) => {
                              if (!canEditBlocks) {
                                errorNoti(t("common:noPermissionToEditBlocks"), 3000);
                                return;
                              }
                              setBlockCodes((prev) => ({
                                ...prev,
                                [selectedLanguage]: prev[selectedLanguage].map((b, i) =>
                                  i === index ? {...b, forStudent: event.target.value === "student"} : b
                                ),
                              }));
                            }}
                            options={[
                              {label: t("common:forTeacher"), value: "teacher"},
                              {label: t("common:forStudent"), value: "student"},
                            ]}
                            sx={{width: "100%"}}
                            disabled={!canEditBlocks}
                          />
                          <Box sx={{
                            display: 'flex',
                            gap: 0.5,
                            justifyContent: 'center',
                            width: '100%',
                            alignItems: 'center'
                          }}>
                            <Tooltip title={t('common:moveUp')} placement="bottom">
                              <IconButton
                                onClick={() => debouncedMoveUp(index)}
                                disabled={!canEditBlocks || index === 0}
                                title={t("common:moveUp")}
                                size="small"
                                color="primary"
                              >
                                <ArrowUpwardIcon fontSize="small"/>
                              </IconButton>
                            </Tooltip>
                            <Tooltip title={t('common:moveDown')} placement="bottom">
                              <IconButton
                                onClick={() => debouncedMoveDown(index)}
                                disabled={!canEditBlocks || index === blockCodes[selectedLanguage].length - 1}
                                title={t("common:moveDown")}
                                size="small"
                                color="primary"
                              >
                                <ArrowDownwardIcon fontSize="small"/>
                              </IconButton>
                            </Tooltip>
                            <Tooltip title={t('common:insertAbove')} placement="bottom">
                              <IconButton
                                onClick={() => handleInsertAbove(index)}
                                disabled={!canEditBlocks}
                                title={t("common:insertAbove")}
                                size="small"
                                color="success"
                              >
                                <KeyboardDoubleArrowUpIcon fontSize="small"/>
                              </IconButton>
                            </Tooltip>
                            <Tooltip title={t('common:insertBelow')} placement="bottom">
                              <IconButton
                                onClick={() => handleInsertBelow(index)}
                                disabled={!canEditBlocks}
                                title={t("common:insertBelow")}
                                size="small"
                                color="success"
                              >
                                <KeyboardDoubleArrowDownIcon fontSize="small"/>
                              </IconButton>
                            </Tooltip>
                            <Tooltip title={t('common:delete')} placement="bottom">
                              <IconButton
                                onClick={() => handleDeleteBlock(index)}
                                disabled={!canEditBlocks}
                                title={t("common:delete")}
                                size="small"
                                color="error"
                              >
                                <DeleteIcon fontSize="small"/>
                              </IconButton>
                            </Tooltip>
                          </Box>
                        </Box>
                      )}
                    </Box>
                  ))
                ) : null
              ) : (
                <Box>
                  <HustCopyCodeBlock
                    text={getCombinedBlockCode()}
                    language={mapLanguageToCodeBlockLanguage(selectedLanguage)}
                    showLineNumbers
                  />
                </Box>
              )}
            </Collapse>
            {canEditBlocks && (
              <Stack direction="row" spacing={2} sx={{marginTop: "12px"}}>
                <TertiaryButton
                  variant="outlined"
                  startIcon={<AddIcon/>}
                  onClick={handleAddBlock}
                  disabled={!canEditBlocks}
                >
                  {t("addProblemBlock")}
                </TertiaryButton>
                <TertiaryButton
                  variant="outlined"
                  startIcon={<ContentCopyIcon/>}
                  onClick={handleCopyAllCode}
                  disabled={!(blockCodes[selectedLanguage]?.length > 0)}
                >
                  {t("common:copyCode")}
                </TertiaryButton>
              </Stack>
            )}
          </Box>
        )}

        {/* File Attachments */}
        <Box sx={{mt: 2}}>
          <Typography variant="body1" sx={{mb: 1, fontWeight: 500}}>
            Tệp đính kèm
          </Typography>
          <HustDropzoneArea
            hideTitle={true}
            showPreviews={true}
            showPreviewsInDropzone={false}
            useChipsForPreview={true}
            onChangeAttachment={(files) => handleAttachmentFiles(files)}
          />

          {/* Display existing files from API */}
          {fetchedImageArray.length > 0 && (
            <Stack spacing={0.5} sx={{mt: 1}}>
              {fetchedImageArray.map((file) => (
                <FileUploadZone
                  key={file.id}
                  file={file}
                  removable={true}
                  downloadable={true}
                  onDownload={handleDownloadFile}
                  onRemove={() => handleDeleteImageAttachment(file.id)}
                />
              ))}
            </Stack>
          )}
        </Box>
      </Box>

      <Box sx={{mt: 3}}/>
      <HustCodeEditor
        title={t("solutionSourceCode") + " *"}
        language={languageSolution}
        onChangeLanguage={(event) => {
          setLanguageSolution(event.target.value);
        }}
        sourceCode={codeSolution}
        onChangeSourceCode={debouncedSolutionCodeChange}
      />

      <LoadingButton
        variant="outlined"
        loading={loading}
        onClick={checkCompile}
        sx={{margin: "12px 0", textTransform: 'none'}}
      >
        {t("checkSolutionCompile")}
      </LoadingButton>

      <CompileStatus
        showCompile={showCompile}
        statusSuccessful={statusSuccessful}
        detail={compileMessage}
      />

      <Box sx={{marginTop: "12px"}}>
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
            onChangeSourceCode={debouncedPreloadCodeChange}
            height="280px"
            placeholder="Write the initial code segment that provided to the participants here"
          />
        )}
      </Box>

      <Box sx={{marginTop: "12px"}}>
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
            onChangeSourceCode={debouncedCheckerCodeChange}
            placeholder={t("checkerSourceCodePlaceholder")}
          />
        )}
      </Box>

      <ListTestCase/>

      <Stack direction="row" spacing={2} mt={2}>
        <TertiaryButton variant="outlined" onClick={handleExit}>
          {t("common:exit")}
        </TertiaryButton>
        <LoadingButton
          variant="contained"
          loading={loading}
          onClick={handleSubmit}
          sx={{textTransform: 'capitalize'}}
        >
          {t("save", {ns: "common"})}
        </LoadingButton>
      </Stack>

      <ModelAddNewTag
        isOpen={openModalAddNewTag}
        handleSuccess={() => {
          successNoti(t("common:addSuccess", {name: t('tag')}), 3000);
          getAllTags(handleGetTagsSuccess);
        }}
        handleClose={() => setOpenModalAddNewTag(false)}
      />
    </ProgrammingContestLayout>
  );
}

const screenName = "SCR_EDIT_PROBLEM";
export default withScreenSecurity(EditProblem, screenName, true);