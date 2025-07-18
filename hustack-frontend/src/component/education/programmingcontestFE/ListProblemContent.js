import {GetApp, Photo} from "@material-ui/icons";
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
import {FaFileArchive, FaFile, FaFileImage, FaFilePdf, FaFileWord, FaFileExcel, FaFilePowerpoint, FaTrash, FaFileImport, FaFileExport } from "react-icons/fa";
import HustDropzoneArea from "component/common/HustDropzoneArea";
import HustCodeEditor from "component/common/HustCodeEditor";
import { ReactComponent as PhotoIcon } from "../../../assets/icons/photo.svg";
import { ReactComponent as PdfIcon } from "../../../assets/icons/pdf.svg";
import { ReactComponent as DocxIcon } from "../../../assets/icons/docx.svg";
import { ReactComponent as XlsxIcon } from "../../../assets/icons/xlsx.svg";
import { ReactComponent as PptxIcon } from "../../../assets/icons/pptx.svg";
import { ReactComponent as ZipIcon } from "../../../assets/icons/zip.svg";
import { ReactComponent as CodeIcon } from "../../../assets/icons/code.svg";

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
  const extension = fileName?.split(".").pop()?.toLowerCase() || "";
  const iconProps = { width: 32, height: 32, style: { marginRight: "8px", fill: "#1976d2" } };

  switch (extension) {
    case "jpg":
    case "jpeg":
    case "png":
      return <PhotoIcon {...iconProps}  />;
    case "pdf":
      return <PdfIcon {...iconProps} />;
    case "docx":
      return <DocxIcon {...iconProps} />;
    case "xlsx":
      return <XlsxIcon {...iconProps} />;
    case "pptx":
      return <PptxIcon {...iconProps} />;
    case "zip":
      return <ZipIcon {...iconProps} />;
    default:
      return <CodeIcon {...iconProps} />;
  }
};

let fileIdCounter = 0;
const generateFileKey = (file) => {
  const uniqueId = fileIdCounter++;
  return `${file.name}-${file.size || uniqueId}-${file.lastModified || uniqueId}`;
};

// Thêm hàm xử lý clear null và sort key cho object JSON
function cleanAndSortJson(obj) {
  if (Array.isArray(obj)) {
    return obj.map(cleanAndSortJson);
  } else if (obj && typeof obj === 'object') {
    const cleaned = {};
    Object.keys(obj)
      .filter(key => obj[key] !== null)
      .sort((a, b) => a.localeCompare(b))
      .forEach(key => {
        cleaned[key] = cleanAndSortJson(obj[key]);
      });
    return cleaned;
  }
  return obj;
}

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
  const lastErrorTimeRef = useRef(0);


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

    e.target.value = null;

    if (!isValidFile(file, true)) {
      setTimeout(() => errorNoti(t("invalidJsonFormat"), 5000), 1000);
      handleImportFormChange("jsonFile", null);
      handleImportFormChange("jsonContent", "");
      return;
    }

    if (!isValidFileSize(file)) {
      setTimeout(() => errorNoti(
        file.size === 0
          ? t("fileEmpty", {file: file.name})
          : t("fileTooLarge", {maxSize: MAX_FILE_SIZE / (1024 * 1024)}),
        5000
      ), 1000);
      handleImportFormChange("jsonFile", null);
      handleImportFormChange("jsonContent", "");
      return;
    }

    try {
      const jsonText = await readJsonFile(file);
      if (!isValidJson(jsonText)) {
        setTimeout(() => errorNoti(t("invalidJsonContent"), 5000), 1000);
        handleImportFormChange("jsonFile", null);
        handleImportFormChange("jsonContent", "");
        return;
      }
      handleImportFormChange("jsonFile", file);
      handleImportFormChange("jsonContent", jsonText);
    } catch (error) {
      console.error("Error reading JSON file:", error);
      setTimeout(() => errorNoti(t("errorReadingFile"), 5000), 1000);
      handleImportFormChange("jsonFile", null);
      handleImportFormChange("jsonContent", "");
    }
  };

  const handleClearJson = () => {
    handleImportFormChange("jsonFile", null);
    handleImportFormChange("jsonContent", "");
    if (importErrors.jsonFile) {
      setImportErrors((prev) => ({...prev, jsonFile: ""}));
    }
  };

  const handleAttachmentChange = (acceptedFiles, rejectedFiles) => {
    const rejected = Array.isArray(rejectedFiles) ? rejectedFiles : [];
    setImportForm((prev) => {
      const currentFiles = prev.attachments || [];
      // Tổng số file sau khi add
      const totalFiles = currentFiles.length + acceptedFiles.length + rejected.length;
      if (totalFiles > MAX_FILES) {
        errorNoti(t('tooManyFiles', {maxFiles: MAX_FILES}), 4000);
        return prev;
      }
      const remainingSlots = MAX_FILES - currentFiles.length;

      // Xử lý các file hợp lệ
      const validFiles = [];
      acceptedFiles.forEach((file) => {
        const extension = `.${file.name.split(".").pop().toLowerCase()}`;
        if (file.size > MAX_FILE_SIZE) {
          errorNoti(t("fileTooLarge", {file: file.name, maxSize: MAX_FILE_SIZE / (1024 * 1024)}), 4000);
        } else if (file.size === 0) {
          errorNoti(t("fileEmpty", {file: file.name}), 4000);
        } else if (!ALLOWED_EXTENSIONS.includes(extension)) {
          errorNoti(t("invalidFileFormat", {file: file.name}), 4000);
        } else {
          validFiles.push(file);
        }
      });

      // Xử lý các file bị reject (ưu tiên kiểm tra quá kích thước)
      rejected.forEach((file) => {
        if (file.size > MAX_FILE_SIZE) {
          errorNoti(t("fileTooLarge", {file: file.name, maxSize: MAX_FILE_SIZE / (1024 * 1024)}), 4000);
        } else if (file.size === 0) {
          errorNoti(t("fileEmpty", {file: file.name}), 4000);
        } else {
          errorNoti(t("invalidFileFormat", {file: file.name}), 4000);
        }
      });

      const updatedAttachments = [...currentFiles, ...validFiles];
      return {
        ...prev,
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
      Object.values(errors).forEach(msg => errorNoti(msg, 4000));
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
  const statuses = getStatuses(t)

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
      cellStyle: {minWidth: 160, textAlign: "center"},
      headerStyle: {textAlign: "center"},
      sorting: false,
      render: (rowData) => (
        <Stack direction="row" spacing={1} justifyContent="center">
          <Tooltip title={t("export")}>
            <IconButton variant="contained" color="primary" onClick={() => onSingleDownload(rowData)}>
              <FaFileExport style={{ color: '#1976d2' }} />
            </IconButton>
          </Tooltip>
          <Tooltip title={t("exportJson")}>
            <IconButton variant="contained" color="primary" onClick={() => onJsonExport(rowData)}>
              <FaFileExport style={{ color: '#43a047' }} />
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
            startIcon={<FaFileImport />}
            variant="outlined"
            onClick={handleOpenImportDialog}
            sx={{textTransform: "none"}}
          >
            {t("import", {entity: ""})}
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
          <Box>
            {/* Block 1: Two input fields */}
            <Stack direction="row" spacing={2} sx={{mb: 3}}>
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
            </Stack>

            {/* Block 2: Resource file (JSON) */}
            <Stack spacing={2} sx={{mb: 0}}>
              <Stack direction="row" alignItems="center" spacing={2}>
                <Typography variant="h6" component="span" sx={{mb: 5, pb: 0, mt: 0, pt: 0, lineHeight: 1}}>
                  {t("resourceFile")}
                </Typography>
                <Box sx={{flexGrow: 1}} />
                <Stack direction="row" spacing={2} mt={2}>
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
              </Stack>
              <HustCodeEditor
                language="JSON"
                hidePlaceholder
                sourceCode={(() => {
                  if (!importForm.jsonContent) return "";
                  try {
                    const parsed = JSON.parse(importForm.jsonContent);
                    const cleaned = cleanAndSortJson(parsed);
                    return JSON.stringify(cleaned, null, 2);
                  } catch {
                    return importForm.jsonContent;
                  }
                })()}
                onChangeSourceCode={() => {}}
                hideLanguagePicker={true}
                readOnly={true}
                theme="github"
                maxLines={14}
                minLines={14}
              />
              {importErrors.jsonFile && (
                <Typography color="error" variant="body2" sx={{mt: 1}}>
                  {importErrors.jsonFile}
                </Typography>
              )}
            </Stack>

            {/* Block 3: Attachments */}
            <Stack spacing={1.5} sx={{mt: 0, pt: 0}}>
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
                showAlerts={false}
                sx={{
                  '& .MuiTypography-h6': { mb: 0, pb: 0, mt: 0, pt: 0, lineHeight: 1 },
                  m: 0, p: 0
                }}
              />
              {importErrors.attachments && (
                <Typography color="error" variant="body2" sx={{mt: 1 }}>
                  {importErrors.attachments}
                </Typography>
              )}
              {importForm.attachments.length > 0 && (
                <Stack spacing={1} sx={{mt: 1, mt: '0px !important'}}>
                  {importForm.attachments.map((file, index) => (
                    <Box
                      key={generateFileKey(file)}
                      sx={{
                        display: "flex",
                        alignItems: "center",
                        padding: "8px",
                        borderRadius: "8px",
                        backgroundColor: '#fafbfc',
                        "&:hover": {backgroundColor: "#f5f5f5"},
                        justifyContent: "space-between",
                      }}
                    >
                      <Box sx={{display: 'flex', alignItems: 'center', minWidth: 0, flex: 1}}>
                        {getFileIcon(file.name)}
                        <Typography
                          variant="body2"
                          sx={{
                            flexGrow: 1,
                            maxWidth: 240,
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                            whiteSpace: "nowrap",
                          }}
                          title={file.name}
                        >
                          {file.name}
                        </Typography>
                      </Box>
                      <IconButton size="small" color="error" onClick={() => handleRemoveAttachment(index)}>
                        <FaTrash />
                      </IconButton>
                    </Box>
                  ))}
                </Stack>
              )}
            </Stack>
          </Box>
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
              {loading ? t("importing") : t("import", {entity: ""})}
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