import { Component } from '@angular/core';

@Component({
  selector: 'app-icono-papelera',
  template: `<svg
    xmlns="http://www.w3.org/2000/svg"
    width="20"
    height="20"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    <path d="M3 6h18M9 6V4h6v2M5 6l1 14h12l1-14M10 10v6M14 10v6" />
  </svg>`,
  styles: ':host{display:inline-flex;align-items:center;justify-content:center}',
})
export class IconoPapelera {}
