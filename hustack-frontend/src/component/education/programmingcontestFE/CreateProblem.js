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
  Typography,
} from "@mui/material";
import React, {useCallback, useEffect, useMemo, useState} from "react";
import "react-draft-wysiwyg/dist/react-draft-wysiwyg.css";
import {useHistory} from "react-router-dom";
import {CompileStatus} from "./CompileStatus";
import {extractErrorMessage, request} from "../../../api";
import {useTranslation} from "react-i18next";
import HustDropzoneArea from "../../common/HustDropzoneArea";
import {errorNoti, successNoti} from "../../../utils/notification";
import HustCodeEditor from "../../common/HustCodeEditor";
import {LoadingButton} from "@mui/lab";
import RichTextEditor from "../../common/editor/RichTextEditor";
import {COMPUTER_LANGUAGES, CUSTOM_EVALUATION, mapLanguageToDisplayName, NORMAL_EVALUATION} from "./Constant";
import {getAllTags} from "./service/TagService";
import ModelAddNewTag from "./ModelAddNewTag";
import AddIcon from '@mui/icons-material/Add';
import KeyboardDoubleArrowDownIcon from '@mui/icons-material/KeyboardDoubleArrowDown';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import KeyboardDoubleArrowUpIcon from '@mui/icons-material/KeyboardDoubleArrowUp';
import DeleteIcon from '@mui/icons-material/Delete';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import ProgrammingContestLayout from "./ProgrammingContestLayout";
import StyledSelect from "../../select/StyledSelect";
import TertiaryButton from "../../button/TertiaryButton";
import FilterByTag from "../../table/FilterByTag";
import withScreenSecurity from "../../withScreenSecurity";
import {v4 as uuidv4} from 'uuid';
import {AntTab, AntTabs} from "component/tab";
import Tooltip from '@mui/material/Tooltip';
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown';
import RotatingIconButton from "../../common/RotatingIconButton";
import {debounce} from "lodash";


export const getLevels = (t) => [
  {
    label: t("education/programmingcontest/problem:easy"),
    value: "easy",
  },
  {
    label: t("education/programmingcontest/problem:medium"),
    value: "medium",
  },
  {
    label: t("education/programmingcontest/problem:hard"),
    value: "hard",
  },
];

export const getPublicOptions = (t) => [
  {
    label: t("common:yes"),
    value: "Y",
  },
  {
    label: t("common:no"),
    value: "N",
  },
];

export const getStatuses = (t) => [
  {
    label: t('open'),
    value: "OPEN",
  },
  {
    label: t('hidden'),
    value: "HIDDEN",
  }
];

const PROGRAMMING_LANGUAGES = Object.keys(COMPUTER_LANGUAGES).map((key) => ({
  label: key,
  value: COMPUTER_LANGUAGES[key],
}));

function CreateProblem() {
  const history = useHistory();

  const {t} = useTranslation(
    ["education/programmingcontest/problem", "common", "validation"]
  );
  const levels = getLevels(t);
  const publicOptions = getPublicOptions(t);
  const statuses = getStatuses(t);

  const [problemId, setProblemID] = useState("");
  const [problemName, setProblemName] = useState("");
  const [timeLimitCPP, setTimeLimitCPP] = useState(1);
  const [timeLimitJAVA, setTimeLimitJAVA] = useState(1);
  const [timeLimitPYTHON, setTimeLimitPYTHON] = useState(1);
  const [memoryLimit, setMemoryLimit] = useState(256);
  const [levelId, setLevelId] = useState("medium");
  const [description, setDescription] = useState("");
  const [solution, setSolution] = useState("");
  const [codeSolution, setCodeSolution] = useState("");
  // const [isPreloadCode, setIsPreloadCode] = useState(false); // Preload Code functionality - DISABLED
  // const [preloadCode, setPreloadCode] = useState(""); // Preload Code functionality - DISABLED
  const [languageSolution, setLanguageSolution] = useState(COMPUTER_LANGUAGES.CPP17);
  const [solutionChecker, setSolutionChecker] = useState("");
  const [solutionCheckerLanguage, setSolutionCheckerLanguage] = useState(COMPUTER_LANGUAGES.CPP17);
  const [isPublic, setIsPublic] = useState('N');
  const [tags, setTags] = useState([]);
  const [selectedTags, setSelectedTags] = useState([]);
  const [status, setStatus] = useState('HIDDEN');
  const [sampleTestCase, setSampleTestCase] = useState(null);

  const [isCustomEvaluated, setIsCustomEvaluated] = useState(false);
  const [compileMessage, setCompileMessage] = useState("");
  const [attachmentFiles, setAttachmentFiles] = useState([]);
  const [showCompile, setShowCompile] = useState(false);
  const [statusSuccessful, setStatusSuccessful] = useState(false);

  const [loading, setLoading] = useState(false);

  const [openModalAddNewTag, setOpenModalAddNewTag] = useState(false);
  const [isProblemBlock, setIsProblemBlock] = useState(false);
  const [blockCodes, setBlockCodes] = useState(
    Object.fromEntries(PROGRAMMING_LANGUAGES.map(({value}) => [value, []]))
  );
  const [selectedLanguage, setSelectedLanguage] = useState(COMPUTER_LANGUAGES.CPP17);
  const [isBlockCodesExpanded, setIsBlockCodesExpanded] = useState(true);
  const [rotationCount, setRotationCount] = useState(0);

  const handleGetTagsSuccess = (res) => setTags(res.data);

  const handleSelectTags = (tags) => {
    setSelectedTags(tags);
  };

  const handleAttachmentFiles = (files) => {
    setAttachmentFiles(files);
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
          setShowCompile(true);
          errorNoti(extractErrorMessage(e) || t("common:error"), 3000);
        }
      },
      body
    );
  };

  const isValidProblemId = () => {
    return new RegExp(/[%^/\\|.?;[\]]/g).test(problemId);
  };
  const hasSpecialCharacterProblemId = () => {
    return !new RegExp(/^[0-9a-zA-Z_-]*$/).test(problemId);
  };

  const hasSpecialCharacterProblemName = () => {
    return !new RegExp(/^[0-9a-zA-Z ]*$/).test(problemName);
  };

  const validateSubmit = () => {
    if (problemId === "") {
      errorNoti(t("missingField", {ns: "validation", fieldName: t("problemId")}), 3000);
      return false;
    }
    if (hasSpecialCharacterProblemId()) {
      errorNoti(t("common:invalidProblemId"), 3000);
      return false;
    }
    if (problemName === "") {
      errorNoti(t("missingField", {ns: "validation", fieldName: t("problemName")}), 3000);
      return false;
    }
    if (timeLimitCPP < 1 || timeLimitJAVA < 1 || timeLimitPYTHON < 1 ||
      timeLimitCPP > 300 || timeLimitJAVA > 300 || timeLimitPYTHON > 300) {
      errorNoti(t("numberBetween", {ns: "validation", fieldName: t("timeLimit"), min: 1, max: 300}), 3000);
      return false;
    }
    if (memoryLimit < 3 || memoryLimit > 1024) {
      errorNoti(t("numberBetween", {ns: "validation", fieldName: t("memoryLimit"), min: 3, max: 1024}), 3000);
      return false;
    }
    if (!statusSuccessful) {
      errorNoti(t("validateSubmit.warningCheckSolutionCompile"), 5000);
      return false;
    }
    if (isProblemBlock && Object.values(blockCodes).every((blocks) => blocks.length === 0)) {
      errorNoti(t("common:noBlockCodesAdded"), 5000);
      return false;
    }
    return true;
  };

  const handleCopyAllCode = () => {
    const blocks = blockCodes[selectedLanguage] || [];
    if (blocks.length === 0) {
      errorNoti(t("common:noBlockCodesToCopy"), 3000);
      return;
    }
    const allCode = blocks.map(block => block.code).join('\n');
    navigator.clipboard.writeText(allCode).then(() => {
      successNoti(t('common:copySuccess'), 2000);
    });
  };

  function handleSubmit() {
    if (!validateSubmit()) return;

    setLoading(true);
    const fileId = attachmentFiles.map((file) => file.name); // Backend sẽ tự động tạo unique filename
    const tagIds = selectedTags.map((tag) => tag.tagId);

    let formattedBlockCodes = [];
    if (isProblemBlock) {
      formattedBlockCodes = Object.keys(blockCodes)
        .filter((language) => blockCodes[language].length > 0)
        .flatMap((language) =>
          blockCodes[language].map((block, index) => ({
            code: block.code,
            forStudent: block.forStudent ? 1 : 0,
            language: language,
          }))
        );
    }

    let body = {
      problemId: problemId,
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
      // isPreloadCode: isPreloadCode, // Preload Code functionality - DISABLED
      // preloadCode: preloadCode, // Preload Code functionality - DISABLED
      solutionChecker: solutionChecker,
      solutionCheckerLanguage: solutionCheckerLanguage,
      isPublic: isPublic === 'Y',
      fileId: fileId,
      scoreEvaluationType: isCustomEvaluated ? CUSTOM_EVALUATION : NORMAL_EVALUATION,
      tagIds: tagIds,
      status: status,
      sampleTestCase: sampleTestCase,
      categoryId: isProblemBlock ? 1 : 0,
      blockCodes: formattedBlockCodes,
    };

    const formData = new FormData();
    formData.append("dto", new Blob([JSON.stringify(body)], {type: 'application/json'}));

    for (const file of attachmentFiles) {
      formData.append("files", file);
    }

    const config = {
      headers: {
        "content-type": "multipart/form-data",
      },
    };

    request(
      "post",
      "/problems",
      (res) => {
        setLoading(false);
        successNoti(t("common:addSuccess", {name: t("problem")}), 3000);
        history.push("/programming-contest/list-problems");
      },
      {
        onError: (e) => {
          errorNoti(extractErrorMessage(e) || t("common:error"), 3000);
          setLoading(false);
        },
      },
      formData,
      config
    );
  }

  const handleExit = () => {
    history.push(`/programming-contest/list-problems`);
  };

  const handleTabChange = (event, newValue) => {
    setSelectedLanguage(newValue);
  };

  const handleDeleteBlock = (index) => {
    setBlockCodes((prev) => ({
      ...prev,
      [selectedLanguage]: prev[selectedLanguage].filter((_, i) => i !== index),
    }));
  };

  const handleMoveUp = useCallback((index) => {
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
  }, [selectedLanguage]);

  const handleMoveDown = useCallback((index) => {
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
  }, [selectedLanguage]);

  const debouncedMoveUp = useMemo(() => debounce((index) => handleMoveUp(index), 300), [handleMoveUp]);
  const debouncedMoveDown = useMemo(() => debounce((index) => handleMoveDown(index), 300), [handleMoveDown]);

  const handleInsertAbove = (index) => {
    setBlockCodes((prev) => {
      const newBlocks = [...prev[selectedLanguage]];
      newBlocks.splice(index, 0, {
        code: null,
        forStudent: false,
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
    setBlockCodes((prev) => {
      const newBlocks = [...prev[selectedLanguage]];
      newBlocks.splice(index + 1, 0, {
        code: null,
        forStudent: false,
        seq: index + 2,
        id: `${selectedLanguage}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`, // Unique ID
      });
      return {
        ...prev,
        [selectedLanguage]: newBlocks.map((block, i) => ({...block, seq: i + 1})),
      };
    });
  };

  const handleAddBlockCode = () => {
    const language = selectedLanguage || COMPUTER_LANGUAGES.CPP17;
    setBlockCodes((prev) => ({
      ...prev,
      [language]: [
        ...(prev[language] || []),
        {
          code: null,
          forStudent: false,
          seq: prev[language].length + 1,
          id: `${language}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`, // Unique ID
        },
      ],
    }));
  };

  // Memoized handler for code changes
  const handleCodeChange = useCallback((newCode, index) => {
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
  }, [selectedLanguage, t]);

  useEffect(() => {
    getAllTags(handleGetTagsSuccess);
  }, []);

  const canEditBlocks = isProblemBlock && blockCodes[selectedLanguage].length > 0;

  return (
    <ProgrammingContestLayout title={t("common:create", {name: t("problem")})} onBack={handleExit}>
      <Typography variant="h6">
        {t("generalInfo")}
      </Typography>

      <Grid container spacing={2} mt={0}>
        <Grid item xs={3}>
          <TextField
            fullWidth
            size='small'
            autoFocus
            required
            id={"problemId"}
            label={t("problemId")}
            value={problemId}
            onChange={(event) => {
              setProblemID(event.target.value);
            }}
            error={hasSpecialCharacterProblemId()}
            helperText={hasSpecialCharacterProblemId() ? t("common:invalidProblemId") : ""}
            sx={{marginBottom: "12px"}}
          />
        </Grid>
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
            key={t("status")}
            label={t("status")}
            options={statuses}
            value={status}
            sx={{minWidth: 'unset', mr: 'unset'}}
            onChange={(event) => {
              setStatus(event.target.value);
            }}
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
              endAdornment: <InputAdornment position="end">s</InputAdornment>
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
              endAdornment: <InputAdornment position="end">s</InputAdornment>
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
              endAdornment: <InputAdornment position="end">s</InputAdornment>
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
            InputProps={{endAdornment: <InputAdornment position="end">MB</InputAdornment>}}
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

        <Grid item xs={6}>
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

      {/*<Link sx={{ mt: 3, display: 'inline-block' }} href="/programming-contest/suggest-problem" target="_blank"*/}
      {/*      underline="hover">*/}
      {/*  <Typography variant="body1" color="primary">*/}
      {/*    Struggling to create a fresh and exciting challenge? Try our new <b>Problem Suggestion</b> feature*/}
      {/*    <Chip label="Beta" color="secondary" variant="outlined" size="small"*/}
      {/*          sx={{ marginLeft: "8px", marginBottom: "8px", fontWeight: "bold" }} />*/}
      {/*  </Typography>*/}
      {/*</Link>*/}

      <Box sx={{mt: 3, mb: 3}}>
        <Typography variant="h6" sx={{marginTop: "8px", marginBottom: "8px"}}>
          {t("common:description")}
        </Typography>
        <RichTextEditor content={description} onContentChange={text => setDescription(text)}/>
        <FormControlLabel
          label={t("problemBlock")}
          control={<Checkbox checked={isProblemBlock} onChange={() => setIsProblemBlock(!isProblemBlock)}/>}
          sx={{mt: 2}}
        />
        {isProblemBlock && (
          <Box sx={{mt: 1}}>
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
              <AntTabs value={selectedLanguage} onChange={handleTabChange} sx={{mb: 1.5}}>
                {PROGRAMMING_LANGUAGES.map((lang) => (
                  <AntTab key={lang.value} label={mapLanguageToDisplayName(lang.value)} value={lang.value}
                          sx={{textTransform: 'none'}}/>
                ))}
              </AntTabs>
              {blockCodes[selectedLanguage].length === 0 ? (
                <Stack direction="row" spacing={2} sx={{marginTop: '16px'}}>
                  <TertiaryButton
                    variant="outlined"
                    startIcon={<AddIcon/>}
                    onClick={handleAddBlockCode}
                  >
                    {t("addProblemBlock")}
                  </TertiaryButton>
                  <TertiaryButton
                    variant="outlined"
                    startIcon={<ContentCopyIcon/>}
                    onClick={handleCopyAllCode}
                    disabled
                  >
                    {t("common:copyCode")}
                  </TertiaryButton>
                </Stack>
              ) : (
                <>
                  {blockCodes[selectedLanguage].map((block, index) => (
                    <Box
                      key={block.id || uuidv4()}
                      sx={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: 2,
                        mb: index !== blockCodes[selectedLanguage].length - 1 ? 1.5 : 0,
                      }}
                    >
                      <Box
                        sx={{
                          width: '48px',
                          minWidth: '48px',
                          display: 'flex',
                          justifyContent: 'center',
                          alignItems: 'flex-start',
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
                        <HustCodeEditor
                          key={(block.id || uuidv4()) + '_' + block.forStudent}
                          sourceCode={block.code || ""}
                          hideProgrammingLanguage={1}
                          theme={block.forStudent ? "github" : "monokai"}
                          minLines={5}
                          onChangeSourceCode={(newCode) => handleCodeChange(newCode, index)}
                          language={block.language || selectedLanguage}
                          height="300px"
                        />
                      </Box>
                      <Box
                        sx={{
                          width: '200px',
                          minWidth: '200px',
                          display: 'flex',
                          flexDirection: 'column',
                          justifyContent: 'center',
                          alignSelf: 'center',
                          gap: 1
                        }}
                      >
                        <StyledSelect
                          size="small"
                          value={block.forStudent ? "student" : "teacher"}
                          onChange={(event) => {
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
                              disabled={index === 0}
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
                              disabled={index === blockCodes[selectedLanguage].length - 1}
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
                              title={t("common:delete")}
                              size="small"
                              color="error"
                            >
                              <DeleteIcon fontSize="small"/>
                            </IconButton>
                          </Tooltip>
                        </Box>
                      </Box>
                    </Box>
                  ))}
                  {canEditBlocks && (
                    <Box sx={{display: 'flex', alignItems: 'center', mt: 2, gap: 2}}>
                      <Box sx={{width: '48px', minWidth: '48px'}}/>
                      <Typography variant="body2" color="warning.main" sx={{ml: 0}}>
                        {t('common:blockCodeAutoRemoveNote')}
                      </Typography>
                    </Box>
                  )}
                  <Stack direction="row" spacing={2} sx={{mt: 2}}>
                    <TertiaryButton
                      variant="outlined"
                      startIcon={<AddIcon/>}
                      onClick={handleAddBlockCode}
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
                </>
              )}
            </Collapse>
          </Box>
        )}
        <Typography variant="body1" sx={{mb: 1, mt: 2}}>{t('common:sampleTestcase')}</Typography>
        <HustCodeEditor
          hideProgrammingLanguage={1}
          placeholder={null}
          sourceCode={sampleTestCase}
          onChangeSourceCode={(code) => {
            setSampleTestCase(code);
          }}
          minLines={15}
        />

        {/* File Attachments */}
        <Typography variant="body1" sx={{mb: 1, mt: 2}}>{t('common:attachments')}</Typography>
        <HustDropzoneArea
          hideTitle={true}
          onChangeAttachment={(files) => handleAttachmentFiles(files)}
        />
      </Box>

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
        minLines={15}
      />
      <LoadingButton
        variant="outlined"
        loading={loading}
        onClick={checkCompile}
        sx={{mt: 1.5, textTransform: 'none'}}
      >
        {t("checkSolutionCompile")}
      </LoadingButton>
      <CompileStatus
        showCompile={showCompile}
        statusSuccessful={statusSuccessful}
        detail={compileMessage}
      />

      {/* Preload Code functionality - DISABLED
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
        {isPreloadCode &&
          <HustCodeEditor
            title={t("preloadCode")}
            sourceCode={preloadCode}
            onChangeSourceCode={(code) => {
              setPreloadCode(code);
            }}
            height="280px"
            placeholder="Write the initial code segment that provided to the participants here"
          />
        }
      </Box>
      */}

      <Box sx={{mt: 2}}>
        <FormControlLabel
          label={t("isCustomEvaluated")}
          control={
            <Checkbox
              checked={isCustomEvaluated}
              onChange={() => setIsCustomEvaluated(!isCustomEvaluated)}
            />
          }
        />
        <Typography variant="body2" color="gray" sx={{mb: 1}}>{t("customEvaluationNote1")}</Typography>

        {isCustomEvaluated && (
          <HustCodeEditor
            customTitle={<Typography variant="body1">{t("checkerSourceCode")}</Typography>}
            language={solutionCheckerLanguage}
            onChangeLanguage={(event) => {
              setSolutionCheckerLanguage(event.target.value);
            }}
            sourceCode={solutionChecker}
            onChangeSourceCode={(code) => {
              setSolutionChecker(code);
            }}
            minLines={15}
          />
        )}
      </Box>

      <Box width="100%" sx={{mt: 2}}>
        <LoadingButton
          variant="contained"
          loading={loading}
          onClick={handleSubmit}
          sx={{textTransform: 'capitalize'}}
        >
          {t("save", {ns: "common"})}
        </LoadingButton>
      </Box>

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

const screenName = "SCR_CREATE_PROBLEM";
export default withScreenSecurity(CreateProblem, screenName, true);