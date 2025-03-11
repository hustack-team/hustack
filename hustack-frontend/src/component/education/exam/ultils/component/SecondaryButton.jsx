
import Button from "@mui/material/Button";
import {yellow} from "@mui/material/colors";
import {styled} from "@mui/material/styles";

const StyledButton = styled(Button)(({ theme }) => ({
  textTransform: "none",
  backgroundColor: yellow[700],
  color: theme.palette.getContrastText(yellow[700]),
  "&:hover": {
    backgroundColor: yellow[700],
  },
}));

const SecondaryButton = (props) => (
  <StyledButton variant="contained" {...props}>
    {props.children}
  </StyledButton>
);

export default SecondaryButton;
