import { TextField, type TextFieldProps } from './text-field';

export interface TextareaFieldProps extends Omit<TextFieldProps, 'multiline'> {
  /** Starting height in lines. The control grows with content rather than clipping it. */
  rows?: number;
}

/**
 * Multi-line text. Section 5.2 prefers wrapping over truncation, so no line limit is imposed
 * and the control is never given a fixed single-line height.
 */
export function TextareaField({ rows = 4, ...rest }: TextareaFieldProps) {
  return <TextField {...rest} multiline numberOfLines={rows} />;
}
