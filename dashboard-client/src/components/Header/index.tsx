import React from "react";
import "./Header.css";

interface HeaderProps {
  title: string;
  subtitle?: string;
}

const Header: React.FC<HeaderProps> = ({ title, subtitle }) => {
  return (
    <header className="header">
      <h1 className="header-title">{title}</h1>
      {subtitle && <h2 className="header-subtitle">{subtitle}</h2>}
    </header>
  );
};

export default Header;
