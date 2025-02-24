program TabletCourse;

var
  N, result: Integer;

procedure FindWays(full, half: Integer);
begin
  if (full = 0) and (half = 0) then
    result += 1
  else
  begin
    if (full > 0) then
      FindWays(full - 1, half + 1); // Вытягиваем целую таблетку
    if (half > 0) then
      FindWays(full, half - 1); // Вытягиваем половину таблетки
  end;
end;

begin
  Write('Введите количество таблеток (N): ');
  ReadLn(N);

  result := 0;
  FindWays(N, 0);
  
  WriteLn('Количество способов пройти курс лечения: ', result);
end.