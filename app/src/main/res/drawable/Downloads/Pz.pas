var massive:array[1..31] of real;
var max, min: real;
var k1,k2:Integer; 
begin
  writeln('Введите температуры');
  for var i := 1 to 31 do
  read(massive[i]);
  max := massive[1];
  min := massive[1];
  for var i :=2 to 31 do
  if massive[i] > max then
    max := massive[i];
    for var i :=2 to 31 do
  if massive[i] < min then
    min := massive[i];
  k1 := 0;
  k2:= 0;
  for var i := 1 to 31 do
  if massive[i] = min then
  k1 +=1;
  for var i := 1 to 31 do
  if massive[i] = max then
  k2 +=1;
  writeln('самая высокая температура ',max);
  writeln('Дней длилась ', k2);
  writeln('Самая низкая температура ',min);
    writeln('Дней длилась ', k1);
end.
