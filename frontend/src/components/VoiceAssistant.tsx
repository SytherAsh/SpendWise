import { useEffect, useState } from 'react';
import { Fab, styled } from '@mui/material';
import MicIcon from '@mui/icons-material/Mic';
import CloseIcon from '@mui/icons-material/Close';

const StyledFab = styled(Fab)(({ theme }) => ({
  position: 'fixed',
  bottom: 24,
  right: 24,
  background: 'linear-gradient(135deg, #4776E6 0%, #8E54E9 100%)',
  color: 'white',
  '&:hover': {
    background: 'linear-gradient(135deg, #4776E6 0%, #8E54E9 100%)',
    transform: 'scale(1.1)',
    boxShadow: '0 8px 20px -6px rgba(142, 84, 233, 0.4)',
  },
}));

declare global {
  interface Window {
    vapiSDK: any;
    vapiInstance: any;
  }
}

const VoiceAssistant = () => {
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    // Configuration for Vapi AI
    const assistant = "2bb33599-178f-4be1-96bf-afd74aa0e0e0";
    const apiKey = "969d188d-61d3-4284-a13b-9c2ec4cc3294";
    const buttonConfig = {};

    // Create and append the script
    const script = document.createElement('script');
    script.src = "https://cdn.jsdelivr.net/gh/VapiAI/html-script-tag@latest/dist/assets/index.js";
    script.defer = true;
    script.async = true;

    script.onload = () => {
      if (window.vapiSDK) {
        window.vapiInstance = window.vapiSDK.run({
          apiKey: apiKey,
          assistant: assistant,
          config: buttonConfig,
        });
      }
    };

    document.body.appendChild(script);

    // Cleanup
    return () => {
      if (window.vapiInstance) {
        window.vapiInstance.destroy();
      }
      document.body.removeChild(script);
    };
  }, []);

  const handleToggle = () => {
    setIsOpen(!isOpen);
    if (window.vapiInstance) {
      if (isOpen) {
        window.vapiInstance.destroy();
      } else {
        window.vapiInstance.open();
      }
    }
  };

  return (
    <StyledFab aria-label="voice assistant" onClick={handleToggle}>
      {isOpen ? <CloseIcon /> : <MicIcon />}
    </StyledFab>
  );
};

export default VoiceAssistant; 