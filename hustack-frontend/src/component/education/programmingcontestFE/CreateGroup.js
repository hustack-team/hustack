// import React, { useState, useEffect, useMemo } from "react";
// import { useHistory } from "react-router-dom";
// import { useTranslation } from "react-i18next";
// import {
//   Box,
//   Grid,
//   TextField,
//   Typography,
//   Autocomplete,
//   ListItem,
//   ListItemAvatar,
//   ListItemText,
//   Avatar,
//   Popper,
//   IconButton,
//   Stack,
// } from "@mui/material";
// import DeleteIcon from "@mui/icons-material/Delete";
// import { autocompleteClasses } from "@mui/material/Autocomplete";
// import { styled } from "@mui/material/styles";
// import { debounce } from "@mui/material/utils";
// import { LoadingButton } from "@mui/lab";
// import { makeStyles } from "@material-ui/core";
// import { request } from "api";
// import { errorNoti, successNoti } from "utils/notification";
// import { sleep } from "./lib";
// import ProgrammingContestLayout from "./ProgrammingContestLayout";
// import StyledSelect from "../../select/StyledSelect";
// import HustCodeEditor from "../../common/HustCodeEditor";
// import FilterByTag from "../../table/FilterByTag";
// import StandardTable from "component/table/StandardTable";
// import TertiaryButton from "../../button/TertiaryButton";
// import withScreenSecurity from "../../withScreenSecurity";
// import { isEmpty, trim } from "lodash";
// import PrimaryButton from "component/button/PrimaryButton";

// const useStyles = makeStyles((theme) => ({
//   description: {
//     marginTop: theme.spacing(3),
//     marginBottom: theme.spacing(3),
//   },
// }));

// const StyledAutocompletePopper = styled(Popper)(({ theme }) => ({
//   [`& .${autocompleteClasses.paper}`]: {
//     boxShadow:
//       "0 12px 28px 0 rgba(0, 0, 0, 0.2), 0 2px 4px 0 rgba(0, 0, 0, 0.1), inset 0 0 0 1px rgba(255, 255, 255, 0.5)",
//     margin: 0,
//     padding: 8,
//     borderRadius: 8,
//   },
//   [`& .${autocompleteClasses.listbox}`]: {
//     padding: 0,
//     [`& .${autocompleteClasses.option}`]: {
//       padding: "0px 8px",
//       borderRadius: 8,
//       "&:hover": {
//         backgroundColor: "#eeeeee",
//       },
//     },
//   },
// }));

// function PopperComponent(props) {
//   return <StyledAutocompletePopper {...props} />;
// }

// function stringToColor(string) {
//   if (!string) return "#000";
//   let hash = 0;
//   for (let i = 0; i < string.length; i += 1) {
//     hash = string.charCodeAt(i) + ((hash << 5) - hash);
//   }
//   let color = "#";
//   for (let i = 0; i < 3; i += 1) {
//     const value = (hash >> (i * 8)) & 0xff;
//     color += `00${value.toString(16)}`.slice(-2);
//   }
//   return color;
// }

// function stringAvatar(id, name) {
//   const text = name
//     ?.split(" ")
//     .filter((word) => word)
//     .map((word) => word[0])
//     .join("")
//     .slice(0, 2) || id.slice(0, 2);
//   return {
//     children: text?.toUpperCase(),
//     sx: {
//       bgcolor: stringToColor(id),
//     },
//   };
// }

// const getStatuses = (t) => [
//   { label: t("common:statusActive"), value: "ACTIVE" },
//   { label: t("common:statusInactive"), value: "INACTIVE" },
// ];


// function CreateGroup() {
//   const { t } = useTranslation(["common", "validation"]);
//   const history = useHistory();
//   const classes = useStyles();

//   const statuses = getStatuses(t);

//   const [groupName, setGroupName] = useState("");
//   const [status, setStatus] = useState("ACTIVE");
//   const [description, setDescription] = useState("");
//   const [memberList, setMemberList] = useState("");
//   const [selectedMembers, setSelectedMembers] = useState([]);
//   const [searchOptions, setSearchOptions] = useState([]);
//   const [keyword, setKeyword] = useState("");
//   const [selectedUsers, setSelectedUsers] = useState([]);
//   const [tags, setTags] = useState([]);
//   const [selectedTags, setSelectedTags] = useState([]);
//   const [loading, setLoading] = useState(false);

//   const isValidGroupName = () => {
//     return new RegExp(/[%^/\\|.?;[\]]/g).test(groupName);
//   };

//   const hasSpecialCharacterGroupName = () => {
//     return !new RegExp(/^[0-9a-zA-Z ]*$/).test(groupName);
//   };

//   const validateSubmit = () => {
//     if (groupName === "") {
//       errorNoti(t("missingField", { ns: "validation", fieldName: t("groupName") }), 3000);
//       return false;
//     }
//     if (hasSpecialCharacterGroupName()) {
//       errorNoti("Group Name must only contain alphanumeric characters and spaces.", 3000);
//       return false;
//     }
//     return true;
//   };

//   const handleGetTagsSuccess = (res) => setTags(res.data);

//   const handleSelectTags = (tags) => {
//     setSelectedTags(tags);
//   };

//   const delayedSearch = useMemo(
//     () =>
//       debounce(({ keyword, exclude }, callback) => {
//         const url = keyword
//           ? `/users?size=10&page=0&keyword=${encodeURIComponent(keyword)}${
//               exclude ? exclude.map((user) => "&exclude=" + user.userName).join("") : ""
//             }`
//           : `/users?size=10&page=0${exclude ? exclude.map((user) => "&exclude=" + user.userName).join("") : ""}`;

//         request(
//           "get",
//           url,
//           (res) => {
//             const data = res.data.content.map((e) => {
//               const user = {
//                 userName: e.userLoginId,
//                 fullName: `${e.firstName || ""} ${e.lastName || ""}`,
//               };
//               if (isEmpty(trim(user.fullName))) {
//                 user.fullName = "Anonymous";
//               }
//               return user;
//             });
//             callback(data);
//           },
//           (error) => {
//             errorNoti("Failed to fetch users", 3000);
//             console.error("Error fetching users:", error);
//           }
//         );
//       }, 400),
//     []
//   );

//   const handleAddMembers = () => {
//     const newMembers = selectedUsers.filter(
//       (user) => !selectedMembers.some((m) => m.userName === user.userName)
//     );
//     setSelectedMembers([...selectedMembers, ...newMembers]);
//     setSelectedUsers([]);
//     setSearchOptions([]);
//     setKeyword("");
//   };

//   const handleRemoveMember = (userName) => {
//     setSelectedMembers(selectedMembers.filter((m) => m.userName !== userName));
//   };

//   const handleSubmit = () => {
//     if (!validateSubmit()) return;

//     setLoading(true);
//     const tagIds = selectedTags.map((tag) => tag.tagId);
//     const manualUserIds = memberList
//       .split("\n")
//       .map((id) => id.trim())
//       .filter((id) => id !== "");
//     const userIds = [...new Set([...selectedMembers.map((m) => m.userName), ...manualUserIds])];

//     let body = {
//       name: groupName,
//       status: status,
//       description: description,
//       userIds: userIds,
//       tagIds: tagIds,
//     };

//     request(
//       "post",
//       "/groups",
//       (res) => {
//         successNoti(t("common:addSuccess", { name: t("group") }), 3000);
//         sleep(1000).then(() => {
//           history.push("/programming-contest/teacher-list-group");
//         });
//       },
//       {
//         onError: (err) => {
//           errorNoti(err?.response?.data?.message || t("common:error"), 5000);
//           setLoading(false);
//         },
//       },
//       body
//     )
//       .then()
//       .finally(() => setLoading(false));
//   };

//   const handleCancel = () => {
//     history.push("/programming-contest/teacher-list-group");
//   };

//   const columns = [
//     {
//       title: t("common:member"),
//       field: "userName",
//       minWidth: 300,
//       render: (rowData) => (
//         <Stack direction="row" alignItems="center">
//           <ListItemAvatar>
//             <Avatar
//               alt="account avatar"
//               {...stringAvatar(rowData.userName, rowData.fullName)}
//             />
//           </ListItemAvatar>
//           <ListItemText
//             primary={rowData.fullName}
//             secondary={rowData.userName}
//           />
//         </Stack>
//       ),
//     },
//     {
//       title: "",
//       render: (row) => (
//         <IconButton
//           onClick={() => handleRemoveMember(row.userName)}
//           disabled={loading}
//           color="error"
//         >
//           <DeleteIcon />
//         </IconButton>
//       ),
//     },
//   ];

//   useEffect(() => {
//     request("get", "/group-tags", handleGetTagsSuccess);
//   }, []);

//   useEffect(() => {
//     const excludeIds = selectedMembers;
//     delayedSearch({ keyword, exclude: excludeIds }, (results) => {
//       let newOptions = [];
//       if (results) {
//         newOptions = [...newOptions, ...results];
//       }
//       setSearchOptions(newOptions);
//     });
//   }, [selectedMembers, keyword, delayedSearch]);

//   return (
//     <ProgrammingContestLayout title={t("common:create", { name: t("group") })} onBack={handleCancel}>
//       <Typography variant="h6">{t("common:generalInfo")}</Typography>

//       <Grid container spacing={2} mt={0}>
//         <Grid item xs={4}>
//           <TextField
//             fullWidth
//             size="small"
//             autoFocus
//             required
//             id="groupName"
//             label={t("groupName")}
//             value={groupName}
//             onChange={(event) => {
//               setGroupName(event.target.value);
//             }}
//             error={isValidGroupName()}
//             helperText={
//               isValidGroupName()
//                 ? "Group Name must not contain special characters including %^/\\|.?;[]"
//                 : ""
//             }
//             sx={{ marginBottom: "12px" }}
//           />
//         </Grid>
//         <Grid item xs={4}>
//           <StyledSelect
//             fullWidth
//             required
//             key={t("status")}
//             label={t("status")}
//             options={statuses}
//             value={status}
//             sx={{ minWidth: "unset", mr: "unset" }}
//             onChange={(event) => {
//               setStatus(event.target.value);
//             }}
//           />
//         </Grid>
//       </Grid>

//       <Box className={classes.description}>
//         <Typography variant="h6" sx={{ marginTop: "8px", marginBottom: "8px" }}>
//           {t("description")}
//         </Typography>
//         <TextField
//           fullWidth
//           multiline
//           rows={4}
//           value={description}
//           id="description"
//           label={t("description")}
//           onChange={(event) => {
//             setDescription(event.target.value);
//           }}
//         />
//       </Box>

//       <Box>
//         <Typography variant="h6" sx={{ marginTop: "8px", marginBottom: "8px" }}>
//           {t("common:addMember")}
//         </Typography>
//         <Stack spacing={2}>
//           <Autocomplete
//             id="add-group-members"
//             multiple
//             fullWidth
//             size="small"
//             PopperComponent={PopperComponent}
//             getOptionLabel={(option) => option.fullName || ""}
//             filterOptions={(x) => x}
//             options={searchOptions}
//             noOptionsText="No matches found"
//             value={selectedUsers}
//             onChange={(event, newValue) => {
//               setSelectedUsers(newValue);
//             }}
//             onInputChange={(event, newInputValue) => {
//               setKeyword(newInputValue);
//             }}
//             renderInput={(params) => (
//               <TextField
//                 {...params}
//                 label={t("common:addMember")}
//                 placeholder={t("common:searchMember")}
//                 inputProps={{
//                   ...params.inputProps,
//                   autoComplete: "new-password",
//                 }}
//                 disabled={loading}
//               />
//             )}
//             renderOption={(props, option) => (
//               <ListItem {...props} key={option.userName} sx={{ p: 0 }}>
//                 <ListItemAvatar>
//                   <Avatar
//                     alt="account avatar"
//                     {...stringAvatar(option.userName, option.fullName)}
//                   />
//                 </ListItemAvatar>
//                 <ListItemText
//                   primary={option.fullName}
//                   secondary={option.userName}
//                 />
//               </ListItem>
//             )}
//           />
//           <PrimaryButton
//             disabled={loading || !selectedUsers.length}
//             onClick={handleAddMembers}
//             sx={{ alignSelf: "flex-start" }}
//           >
//             {t("common:add", { name: "User" })}
//           </PrimaryButton>
//         </Stack>
//         <StandardTable
//           title={t("common:groupMember")}
//           columns={columns}
//           data={selectedMembers}
//           hideCommandBar
//           options={{
//             selection: false,
//             pageSize: 5,
//             search: false,
//             sorting: true,
//           }}
//         />
//       </Box>

//       <Box width="100%" sx={{ marginTop: "20px" }}>
//         <Stack direction="row" spacing={2.5} justifyContent="flex-start">
//           <LoadingButton
//             variant="contained"
//             loading={loading}
//             onClick={handleSubmit}
//             sx={{ textTransform: "capitalize" }}
//             disabled={isValidGroupName() || loading || !groupName.trim()}
//           >
//             {t("save", { ns: "common" })}
//           </LoadingButton>
//           <TertiaryButton
//             variant="outlined"
//             onClick={handleCancel}
//             sx={{ textTransform: "capitalize" }}
//           >
//             {t("cancel", { ns: "common" })}
//           </TertiaryButton>
//         </Stack>
//       </Box>
//     </ProgrammingContestLayout>
//   );
// }

// const screenName = "SCR_CREATE_TEACHER_GROUP";
// export default withScreenSecurity(CreateGroup, screenName, true);