import {IconButton} from '@mui/material';
import {styled} from '@mui/material/styles';

const RotatingIconButton = styled(IconButton)(({ theme, rotation }) => ({
  transform: `rotate(${rotation}deg)`,
  transition: 'transform 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
}));

export default RotatingIconButton; 