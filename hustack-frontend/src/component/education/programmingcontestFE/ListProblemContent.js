import {GetApp} from "@material-ui/icons";
import AddIcon from "@material-ui/icons/Add";
import {Box, Chip, Divider, Grid, IconButton, Paper, Stack, TextField, Tooltip, Typography,} from "@mui/material";
import {request, saveFile} from "api";
import React, {useEffect, useState, useRef} from "react";
import {useTranslation} from "react-i18next";
import {Link} from "react-router-dom";
import {toFormattedDateTime} from "utils/dateutils";
import {errorNoti, successNoti} from "utils/notification";
import StandardTable from "component/table/StandardTable";
import {getColorLevel, getColorStatus} from "./lib";
import FilterByTag from "component/table/FilterByTag";
import PrimaryButton from "../../button/PrimaryButton";
import SearchIcon from "@mui/icons-material/Search";
import AutorenewIcon from "@mui/icons-material/Autorenew";
import TertiaryButton from "../../button/TertiaryButton";
import StyledSelect from "../../select/StyledSelect";
import {useKeycloak} from "@react-keycloak/web";
import {getLevels, getStatuses} from "./CreateProblem";
import CustomizedDialogs from "component/dialog/CustomizedDialogs";
import {BsFiletypeJson} from "react-icons/bs";
import {FaFileArchive, FaFile, FaFileImage, FaFilePdf, FaFileWord, FaFileExcel, FaFilePowerpoint, FaTrash } from "react-icons/fa";
import HustDropzoneArea from "component/common/HustDropzoneArea";
import HustCodeEditor from "component/common/HustCodeEditor";

const MAX_FILE_SIZE = 10 * 1024 * 1024;
const MAX_FILES = 5;
const ALLOWED_MIME_TYPES = [
  "image/jpeg",
  "image/png",
  "application/pdf",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "application/vnd.openxmlformats-officedocument.presentationml.presentation",
  "application/zip",
];
const ALLOWED_EXTENSIONS = [".jpg", ".jpeg", ".png", ".pdf", ".docx", ".xlsx", ".pptx"];
const JSON_MIME_TYPE = "application/json";
const JSON_EXTENSION = ".json";

const filterInitValue = {levelIds: [], tags: [], name: "", statuses: []}

export const selectProps = (options) => ({
  multiple: true,
  renderValue: (selected) => (
    <Box sx={{display: 'flex', flexWrap: 'wrap', gap: 0.5}}>
      {selected.map((value) => (
        <Chip
          size="small"
          key={value}
          label={options.find(item => item.value === value).label}
          sx={{
            marginRight: "6px",
            marginBottom: "6px",
            border: "1px solid lightgray",
            fontStyle: "italic",
          }}
        />
      ))}
    </Box>
  )
})

const getFileIcon = (fileName) => {
  const extension = fileName.split(".").pop().toLowerCase();
  const iconProps = {size: 16, style: {marginRight: "8px", color: "#1976d2"}};

  switch (extension) {
    case "jpg":
    case "jpeg":
    case "png":
      return <FaFileImage {...iconProps} />;
    case "pdf":
      return <FaFilePdf {...iconProps} />;
    case "docx":
      return <FaFileWord {...iconProps} />;
    case "xlsx":
      return <FaFileExcel {...iconProps} />;
    case "pptx":
      return <FaFilePowerpoint {...iconProps} />;
    default:
      return <FaFile {...iconProps} />;
  }
};

let fileIdCounter = 0;
const generateFileKey = (file) => {
  const uniqueId = fileIdCounter++;
  return `${file.name}-${file.size || uniqueId}-${file.lastModified || uniqueId}`;
};

function ListProblemContent({type}) {
  const {keycloak} = useKeycloak();
  const [problems, setProblems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(5);
  const [totalCount, setTotalCount] = useState(0);
  const [filter, setFilter] = useState(filterInitValue);
  const [openImportDialog, setOpenImportDialog] = useState(false);
  const [importForm, setImportForm] = useState({
    problemId: "",
    problemName: "",
    jsonFile: null,
    jsonContent: "",
    attachments: [],
  });
  const [importErrors, setImportErrors] = useState({});
  const dropzoneRef = useRef(null);


  const handleChangePage = (newPage) => {
    setPage(newPage);
  };

  const handleChangePageSize = (newSize) => {
    setPage(0)
    setPageSize(newSize)
  }

  const handleSelectLevels = (event) => {
    setFilter(prevFilter => ({...prevFilter, levelIds: event.target.value}));
  };

  const handleSelectTags = (tags) => {
    setFilter(prevFilter => ({...prevFilter, tags: tags}));
  }

  const handleChangeProblemName = (event) => {
    setFilter(prevFilter => ({...prevFilter, name: event.target.value}));
  }

  const handleChangeStatus = (event) => {
    setFilter(prevFilter => ({...prevFilter, statuses: event.target.value}));
  }

  const resetFilter = () => {
    setFilter(filterInitValue)
  }

  const hasSpecialCharacterProblemId = (value) => {
    return !new RegExp(/^[0-9a-zA-Z_-]*$/).test(value);
  };

  const hasSpecialCharacterProblemName = (value) => {
    return !new RegExp(/^[0-9a-zA-Z ]*$/).test(value);
  };

  const handleOpenImportDialog = () => {
    setOpenImportDialog(true);
  };

  const handleCloseImportDialog = () => {
    setOpenImportDialog(false);
    setImportForm({
      problemId: "",
      problemName: "",
      jsonFile: null,
      jsonContent: "",
      attachments: [],
    });
    setImportErrors({});
    fileIdCounter = 0;
  };

  const handleImportFormChange = (field, value) => {
    setImportForm((prev) => {
      const newForm = {...prev, [field]: value};
      return newForm;
    });
    if (importErrors[field]) {
      setImportErrors((prev) => ({
        ...prev,
        [field]: "",
      }));
    }
  };

  const readJsonFile = (file) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (event) => resolve(event.target.result);
      reader.onerror = (error) => reject(error);
      reader.readAsText(file);
    });
  };

  const isValidFile = (file, isJson = false) => {
    if (!file || !file.name) return false;
    const extension = `.${file.name.split(".").pop().toLowerCase()}`;
    if (isJson) {
      return file.type === JSON_MIME_TYPE && extension === JSON_EXTENSION;
    }
    return ALLOWED_MIME_TYPES.includes(file.type) && ALLOWED_EXTENSIONS.includes(extension);
  };

  const isValidFileSize = (file) => {
    return file && file.size <= MAX_FILE_SIZE && file.size > 0;
  };

  const isValidJson = (text) => {
    try {
      JSON.parse(text);
      return true;
    } catch (e) {
      return false;
    }
  };

  const handleFileInputChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!isValidFile(file, true)) {
      setImportErrors((prev) => ({
        ...prev,
        jsonFile: t("invalidJsonFormat"),
      }));
      return;
    }

    if (!isValidFileSize(file)) {
      setImportErrors((prev) => ({
        ...prev,
        jsonFile: file.size === 0
          ? t("fileEmpty", {file: file.name})
          : t("fileTooLarge", {maxSize: MAX_FILE_SIZE / (1024 * 1024)}),
      }));
      return;
    }

    try {
      const jsonText = await readJsonFile(file);
      if (!isValidJson(jsonText)) {
        setImportErrors((prev) => ({
          ...prev,
          jsonFile: t("invalidJsonContent"),
        }));
        return;
      }
      handleImportFormChange("jsonFile", file);
      handleImportFormChange("jsonContent", jsonText);
    } catch (error) {
      console.error("Error reading JSON file:", error);
      setImportErrors((prev) => ({
        ...prev,
        jsonFile: t("errorReadingFile"),
      }));
    }
  };

  const handleClearJson = () => {
    handleImportFormChange("jsonFile", null);
    handleImportFormChange("jsonContent", "");
    if (importErrors.jsonFile) {
      setImportErrors((prev) => ({...prev, jsonFile: ""}));
    }
  };

  const handleAttachmentChange = (acceptedFiles, rejectedFiles = []) => {
    console.log('handleAttachmentChange called:', {acceptedFiles, rejectedFiles});

    setImportForm((prev) => {
      const currentFiles = prev.attachments || [];
      const remainingSlots = MAX_FILES - currentFiles.length;

      const newState = {
        ...prev,
        errors: {...prev.errors, attachments: ""}
      };

      // Handle rejected files
      if (rejectedFiles.length > 0) {
        const errorMessages = rejectedFiles.map((rejected) => {
          const file = rejected.file;
          if (rejected.errors.some((err) => err.code === "file-too-large")) {
            return t("fileTooLarge", {file: file.name, maxSize: MAX_FILE_SIZE / (1024 * 1024)});
          }
          if (rejected.errors.some((err) => err.code === "file-invalid-type")) {
            return t("invalidFileFormat", {file: file.name});
          }
          if (rejected.errors.some((err) => err.code === "too-many-files")) {
            return t("tooManyFiles", {maxFiles: MAX_FILES});
          }
          if (rejected.errors.some((err) => err.code === "file-too-small")) {
            return t("fileEmpty", {file: file.name });
          }
          return t("errorProcessingFile", {file: file.name});
        });

        return {
          ...newState,
          errors: {...newState.errors, attachments: errorMessages.join("; ")}
        };
      }

      // If no accepted files, return the current state
      if (!acceptedFiles || acceptedFiles.length === 0) {
        return newState;
      }

      // Validate accepted files
      const validFiles = [];
      const invalidFiles = [];

      acceptedFiles.forEach((file) => {
        if (file.size > MAX_FILE_SIZE) {
          invalidFiles.push({name: file.name, error: t("fileTooLarge", {file: file.name, maxSize: MAX_FILE_SIZE / (1024 * 1024) }) });
        } else if (file.size === 0) {
          invalidFiles.push({name: file.name, error: t("fileEmpty", {file: file.name})});
        } else {
          const extension = `.${file.name.split(".").pop().toLowerCase()}`;
          if (!ALLOWED_EXTENSIONS.includes(extension)) {
            invalidFiles.push({name: file.name, error: t("invalidFileFormat", {file: file.name})});
          } else {
            validFiles.push(file);
          }
        }
      });

      // Notify about invalid files
      invalidFiles.forEach(({name, error}) => {
        errorNoti(error, 5000);
      });

      // Update attachments with valid files
      const updatedAttachments = [...currentFiles, ...validFiles];

      // Check if we exceed the maximum number of files
      if (updatedAttachments.length > MAX_FILES) {
        errorNoti(t("tooManyFiles", {maxFiles: MAX_FILES}), 5000);
        return {
          ...newState,
          errors: {...newState.errors, attachments: t("tooManyFiles", {maxFiles: MAX_FILES})},
          attachments: currentFiles // Keep the current files
        };
      }

      return {
        ...newState,
        attachments: updatedAttachments
      };
    });

    if (importErrors.attachments) {
      setImportErrors((prev) => ({...prev, attachments: ""}));
    }
  };


  const handleRemoveAttachment = (index) => {
    setImportForm((prev) => {
      const updatedAttachments = prev.attachments.filter((_, i) => i !== index);
      return {
        ...prev,
        attachments: updatedAttachments,
      };
    });
    if (importErrors.attachments) {
      setImportErrors((prev) => ({...prev, attachments: ""}));
    }
  };

  const validateImportForm = () => {
    let isValid = true;
    const errors = {};

    if (!importForm.problemId) {
      errors.problemId = t("problemIdRequired");
      isValid = false;
    } else if (hasSpecialCharacterProblemId(importForm.problemId)) {
      errors.problemId = t("problemIdInvalid");
      isValid = false;
    }

    if (!importForm.problemName) {
      errors.problemName = t("problemNameRequired");
      isValid = false;
    } else if (hasSpecialCharacterProblemName(importForm.problemName)) {
      errors.problemName = t("problemNameInvalid");
      isValid = false;
    }

    if (!importForm.jsonFile || !importForm.jsonContent) {
      errors.jsonFile = t("jsonFileRequired");
      isValid = false;
    } else if (!isValidJson(importForm.jsonContent)) {
      errors.jsonFile = t("invalidJsonContent");
      isValid = false;
    }

    if (!isValid) {
      setImportErrors(errors);
    }

    return isValid;
  };

  const handleImportSubmit = () => {
    if (!validateImportForm()) {
      return;
    }

    let problemDetailData;
    try {
      problemDetailData = JSON.parse(importForm.jsonContent);
      problemDetailData.problemId = importForm.problemId;
      problemDetailData.problemName = importForm.problemName;
      problemDetailData.fileId = importForm.attachments.map((file) => file.name);
      if (problemDetailData.appearances == null) {
        problemDetailData.appearances = 0;
      }
      if (problemDetailData.isPublic !== undefined) {
        problemDetailData.isPublicProblem = problemDetailData.isPublic;
        delete problemDetailData.isPublic;
      }
    } catch (e) {
      console.error("JSON parsing error in submit:", e);
      setImportErrors((prev) => ({
        ...prev,
        jsonContent: t("invalidJsonContent"),
      }));
      return;
    }

    const formData = new FormData();
    formData.append("problemDetail", new Blob([JSON.stringify(problemDetailData)], {type: "application/json"}));
    for (const file of importForm.attachments) {
      formData.append("files", file);
    }

    setLoading(true);
    const config = {
      headers: {
        "content-type": "multipart/form-data",
      },
    };

    request(
      "post",
      "/problems/import",
      (res) => {
        setLoading(false);
        successNoti(t("importSuccess"), 3000);
        handleCloseImportDialog();
        handleSearch();
      },
      {
        onError: (e) => {
          setLoading(false);
          let errorMessage = t("common:error");

          if (e.response?.data) {
            const errorData = e.response.data;

            if (typeof errorData === "string") {
              errorMessage = t(errorData, {defaultValue: errorData});
            } else if (errorData.message) {
              errorMessage = t(errorData.message, {defaultValue: errorData.message});
            } else if (errorData.error) {
              errorMessage = t(errorData.error, {defaultValue: errorData.error});
            } else if (errorData.errors && Array.isArray(errorData.errors)) {
              errorMessage = errorData.errors.map((err) => t(err.message, {defaultValue: err.message})).join("; ");
            }
            if (errorData.message && errorData.message.includes("Too many files")) {
              errorMessage = t("tooManyFiles", {maxFiles: MAX_FILES});
            }
            if (errorData.message && errorData.message.includes("size exceeds")) {
              errorMessage = t("fileTooLarge", {maxSize: MAX_FILE_SIZE / (1024 * 1024)});
            }
            if (errorData.message && errorData.message.includes("is empty")) {
              errorMessage = t("fileEmpty");
            }
            if (errorData.message && errorData.message.includes("Failed to store file")) {
              errorMessage = t("failedToStoreFile");
            }
            if (errorData === "Problem ID or name already exists") {
              errorMessage = t("problemIdNameExists");
            }
          }

          console.error("Import error:", e.response?.data || e);
          errorNoti(errorMessage, 5000);
        },
      },
      formData,
      config
    );
  };

  const handleSearch = () => {
    setLoading(true);
    let url;
    switch (type) {
      case 0:
        url = `/teacher/owned-problems?page=${page}&size=${pageSize}`;
        break
      case 1:
        url = `/teacher/shared-problems?page=${page}&size=${pageSize}`;
        break
      case 2:
        url = `/teacher/public-problems?page=${page}&size=${pageSize}`;
        break
    }

    if (filter.name) {
      url += `&name=${filter.name}`;
    }
    url += `&levelIds=${filter.levelIds}`;
    url += `&tagIds=${filter.tags.map(item => item.tagId)}`;
    url += `&statusIds=${filter.statuses}`;

    request("get",
      url,
      (res) => {
        setLoading(false);

        const data = res.data
        const myProblems = data.content

        if (data.numberOfElements === 0 && data.number > 0) {
          setPage(0)
        } else {
          setProblems(myProblems);
          setTotalCount(data.totalElements)
        }
      },
      {
        onError: (e) => {
          setLoading(false);
          errorNoti(t("common:error", 3000))
        }
      });
  }
  
  const {t} = useTranslation(["education/programmingcontest/problem", "common"]);
  const levels = getLevels(t);
  const statuses = getStatuses(t);

  const onSingleDownload = async (problem) => {
    request("GET",
      `/problems/${problem.problemId}/export`,
      (res) => {
        saveFile(`${problem.problemId}.zip`, res.data);
      },
      {
        onError: (e) => {
          errorNoti(t("common:error", 3000))
        },
      },
      {},
      {responseType: "blob"}
    );
  };

  const onJsonExport = async (problem) => {
    request(
      "get",
      `/problems/${problem.problemId}/export/json`,
      (res) => {
        saveFile(`${problem.problemId}.zip`, res.data);
      },
      {
        onError: (e) => {
          errorNoti(t("common:error"), 3000);
        },
      },
      {},
      {responseType: "blob"}
    );
  };

  const COLUMNS = [
    {
      title: "ID",
      field: "problemId",
      cellStyle: {minWidth: 300},
      render: (rowData) => (
        <Link
          to={{
            pathname:
              "/programming-contest/manager-view-problem-detail/" +
              encodeURIComponent(rowData["problemId"]),
          }}
          style={{
            textDecoration: "none",
            color: "blue",
            cursor: "pointer",
          }}
        >
          {rowData["problemId"]}
        </Link>
      ),
    },
    {title: t("problemName"), field: "problemName", cellStyle: {minWidth: 300}},
    {title: t("common:createdBy"), field: "userId", cellStyle: {minWidth: 120}},
    {
      title: t("level"),
      field: "levelId",
      align: 'center',
      cellStyle: {minWidth: 120},
      render: (rowData) => (
        <Typography component="span" variant="subtitle2" sx={{color: getColorLevel(`${rowData.levelId}`)}}>
          {`${levels.find(item => item.value === rowData.levelId)?.label || ""}`}
        </Typography>
      ),
    },
    {
      title: t("status"),
      field: "statusId",
      align: 'center',
      cellStyle: {minWidth: 120},
      render: (rowData) => (
        <Typography component="span" variant="subtitle2" sx={{color: getColorStatus(`${rowData.statusId}`)}}>
          {`${statuses.find(item => item.value === rowData.statusId)?.label || ''}`}
        </Typography>
      )
    },
    {
      title: t("tag"),
      fields: "tags",
      render: (rowData) => (
        <Box>
          {rowData.tags?.length > 0 &&
            rowData.tags.map((tag) => (
              <Chip
                size="small"
                label={tag.name}
                key={tag.tagId}
                sx={{
                  marginRight: "6px",
                  marginBottom: "6px",
                  border: "1px solid lightgray",
                  fontStyle: "italic",
                }}
              />
            ))}
        </Box>
      ),
    },
    {
      title: t("appearances"),
      field: "appearances",
      align: 'right',
      cellStyle: {minWidth: 200},
      render: (rowData) => {
        return (
          <span style={{marginLeft: "24px"}}>{rowData.appearances}</span>
        );
      },
    },
    {
      title: t("common:createdTime"),
      field: "createdAt",
      cellStyle: {minWidth: 200},
      render: (rowData) => toFormattedDateTime(rowData.createdAt)
    },
    {
      title: t("common:action"),
      cellStyle: {minWidth: 160},
      render: (rowData) => (
        <Stack direction="row" spacing={1}>
          <Tooltip title={t("export")}>
            <IconButton variant="contained" color="primary" onClick={() => onSingleDownload(rowData)}>
              <FaFileArchive />
            </IconButton>
          </Tooltip>
          <Tooltip title={t("exportJson")}>
            <IconButton variant="contained" color="primary" onClick={() => onJsonExport(rowData)}>
              <BsFiletypeJson />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ];

  useEffect(() => {
    handleSearch()
  }, [page, pageSize, type]);

  return (
    <Paper elevation={1} sx={{padding: "16px 24px", borderRadius: 4}}>
      <Typography variant="h6" sx={{marginBottom: "12px"}}>{t("search")}</Typography>
      <Grid container spacing={3}>
        <Grid item xs={3}>
          <TextField
            size='small'
            fullWidth
            label={t("problemName")}
            value={filter.name}
            onChange={handleChangeProblemName}
          />
        </Grid>
        <Grid item xs={3}>
          <StyledSelect
            fullWidth
            key={t("level")}
            label={t("level")}
            options={levels}
            value={filter.levelIds}
            sx={{minWidth: 'unset', mr: 'unset'}}
            SelectProps={selectProps(levels)}
            onChange={(event) => {
              handleSelectLevels(event);
            }}
          />
        </Grid>
        <Grid item xs={3}>
          <FilterByTag onSelect={handleSelectTags} value={filter.tags}/>
        </Grid>
        <Grid item xs={3}>
          <StyledSelect
            fullWidth
            key={t("status")}
            label={t("status")}
            options={statuses}
            value={filter.statuses}
            sx={{minWidth: 'unset', mr: 'unset'}}
            SelectProps={selectProps(statuses)}
            onChange={(event) => {
              handleChangeStatus(event);
            }}
          />
        </Grid>
      </Grid>
      <Stack direction="row" justifyContent='flex-end' spacing={2} sx={{mt: 3}}>
        <TertiaryButton
          onClick={resetFilter}
          variant="outlined"
          startIcon={<AutorenewIcon/>}
          sx={{textTransform: "none"}}
        >
          {t("reset")}
        </TertiaryButton>
        <PrimaryButton
          disabled={loading}
          onClick={handleSearch}
          startIcon={<SearchIcon/>}
          sx={{textTransform: "none"}}
        >
          {t("search")}
        </PrimaryButton>
      </Stack>

      <Divider sx={{mt: 2, mb: 2}}/>

      <Stack direction="row" justifyContent='space-between' mb={1.5}>
        <Typography variant="h6">{t("problemList")}</Typography>
        <Stack direction="row" spacing={2}>
          <TertiaryButton
            variant="outlined"
            onClick={handleOpenImportDialog}
            sx={{textTransform: "none"}}
          >
            {t("import")}
          </TertiaryButton>
          <PrimaryButton
            startIcon={<AddIcon />}
            onClick={() => window.open("/programming-contest/create-problem")}
            sx={{textTransform: "none"}}
          >
            {t("common:create", {name: ""})}
          </PrimaryButton>
        </Stack>
      </Stack>

      <CustomizedDialogs

        open={openImportDialog}
        handleClose={handleCloseImportDialog}
        maxWidth="md"
        fullWidth
        contentTopDivider={true}
        title={t("importProblem")}
        content={
          <Grid container spacing={3}>
            <Grid container item xs={12} spacing={2} mb={1}>
              <Grid item xs={6}>
                <TextField
                  autoFocus
                  margin="dense"
                  label={t("problemId")}
                  type="text"
                  fullWidth
                  size="small"
                  required
                  variant="outlined"
                  value={importForm.problemId}
                  onChange={(e) => handleImportFormChange("problemId", e.target.value)}
                  error={!!importErrors.problemId || hasSpecialCharacterProblemId(importForm.problemId)}
                  helperText={
                    importErrors.problemId ||
                    (hasSpecialCharacterProblemId(importForm.problemId) ? t("problemIdInvalid") : "")
                  }
                />
              </Grid>
              <Grid item xs={6}>
                <TextField
                  margin="dense"
                  label={t("problemName")}
                  type="text"
                  fullWidth
                  size="small"
                  required
                  variant="outlined"
                  value={importForm.problemName}
                  onChange={(e) => handleImportFormChange("problemName", e.target.value)}
                  error={!!importErrors.problemName || hasSpecialCharacterProblemName(importForm.problemName)}
                  helperText={
                    importErrors.problemName ||
                    (hasSpecialCharacterProblemName(importForm.problemName) ? t("problemNameInvalid") : "")
                  }
                />
              </Grid>
            </Grid>

            <Grid item xs={12} sx={{mt: -1, mb: -5}}>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={12} md={4}>
                  <Typography variant="h6" component="span">
                    {t("resourceFile")}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={12} md={8}>
                  <Stack direction="row" spacing={2} justifyContent="flex-end">
                    <TertiaryButton
                      onClick={handleClearJson}
                      disabled={!importForm.jsonFile}
                      sx={{textTransform: "none"}}
                      color="error"
                    >
                      {t("common:delete")}
                    </TertiaryButton>
                    <PrimaryButton variant="outlined" component="label" sx={{textTransform: "none"}}>
                      {t("selectJsonFile")}
                      <input type="file" hidden accept=".json" onChange={handleFileInputChange} />
                    </PrimaryButton>
                  </Stack>
                </Grid>
              </Grid>
            </Grid>

            <Grid item xs={12} sx={{mt: 2, mb: -3}}>
              <HustCodeEditor
                language="JSON"
                hidePlaceholder
                sourceCode={importForm.jsonContent}
                onChangeSourceCode={() => {}}
                hideLanguagePicker={true}
                readOnly={true}
                theme="github"
                maxLines={15}
                minLines={15}
              />
              {importErrors.jsonFile && (
                <Typography color="error" variant="body2" sx={{mt: 1}}>
                  {importErrors.jsonFile}
                </Typography>
              )}
            </Grid>

            <Grid item xs={12}>
              <HustDropzoneArea
                ref={dropzoneRef}
                key={`dropzone-${importForm.attachments.length}`}
                onChangeAttachment={handleAttachmentChange}
                acceptedFiles={ALLOWED_EXTENSIONS}
                filesLimit={MAX_FILES}
                maxFileSize={MAX_FILE_SIZE}
                title={t("attachments")}
                hideFileList={true}
                initialFiles={[]}
                hideLogo={true}
              />
              {importErrors.attachments && (
                <Typography color="error" variant="body2" sx={{mt: 1 }}>
                  {importErrors.attachments}
                </Typography>
              )}
            </Grid>

            {importForm.attachments.length > 0 && (
              <Grid item xs={12}>
                <Stack spacing={1}>
                  {importForm.attachments.map((file, index) => (
                    <Box
                      key={generateFileKey(file)}
                      sx={{
                        display: "flex",
                        alignItems: "center",
                        padding: "8px",
                        borderRadius: "4px",
                        "&:hover": {backgroundColor: "#f5f5f5"},
                      }}
                    >
                      {getFileIcon(file.name)}
                      <Typography variant="body2" sx={{flexGrow: 1}}>
                        {file.name}
                      </Typography>
                      <IconButton size="small" color="error" onClick={() => handleRemoveAttachment(index)}>
                        <FaTrash />
                      </IconButton>
                    </Box>
                  ))}
                </Stack>
              </Grid>
            )}
          </Grid>
        }

        actions={
          <Stack direction="row" spacing={2}>
            <TertiaryButton variant="outlined" onClick={handleCloseImportDialog} sx={{textTransform: "none" }}>
              {t("common:cancel")}
            </TertiaryButton>
            <PrimaryButton
              onClick={handleImportSubmit}
              disabled={loading}
              sx={{textTransform: "none"}}
            >
              {loading ? t("importing") : t("import")}
            </PrimaryButton>
          </Stack>
        }
      />

      <StandardTable
        columns={COLUMNS.filter(item => {
          if (type === 0) {
            return item.field !== 'userId'
          } else {
            return true
          }
        })}
        data={problems}
        hideCommandBar
        hideToolBar
        options={{
          selection: false,
          pageSize: pageSize,
          search: false,
          sorting: false,
        }}
        components={{
          Container: (props) => <Paper {...props} elevation={0}/>,
        }}
        isLoading={loading}
        page={page}
        totalCount={totalCount}
        onChangePage={handleChangePage}
        onChangeRowsPerPage={handleChangePageSize}
      />
    </Paper>
  );
}

export default ListProblemContent